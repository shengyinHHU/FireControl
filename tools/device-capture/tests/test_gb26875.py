import datetime as dt
import importlib.util
import io
from pathlib import Path
import sys
import unittest
from collections import deque
from unittest.mock import Mock

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from gb26875 import FrameParser, RecentFrames, decode_frame, encode_frame, make_confirmation
from tcp_capture import Capture

# Three distinct frames from the user's 2026-10-20 capture (no site/contact data).
SAMPLES = [bytes.fromhex(h) for h in [
    "404037000102380813090311c80000000000640000000000000002d82323",
    "4040390001020b0a13090311c80000000000640000000000000002af2323",
    "404040000102100b13090311c80000000000640000000000000002bc2323"
]]


class ProtocolTests(unittest.TestCase):
    def test_captured_frames_and_fixed_confirmation_fields(self):
        now = dt.datetime(2026, 10, 20, 17, 40, 1)
        for frame, sequence in zip(SAMPLES, [55, 57, 64]):
            info = decode_frame(frame)
            self.assertEqual(info["sequence"], sequence)
            self.assertEqual(info["source_address"], 200)
            self.assertEqual(info["destination_address"], 100)
            self.assertEqual(info["application_length"], 0)
            reply = make_confirmation(frame, now)
            expected = (b"@@" + frame[2:6] + bytes([1, 40, 17, 20, 10, 26])
                        + frame[18:24] + frame[12:18] + b"\x00\x00\x03")
            expected += bytes([sum(expected[2:]) % 256]) + b"##"
            self.assertEqual(reply, expected)
            ack = decode_frame(reply)
            self.assertEqual(ack["command"], 3)
            self.assertEqual(ack["sequence"], sequence)
            self.assertEqual(ack["device_time"], "2026-10-20T17:40:01")
            with self.assertRaises(ValueError):
                make_confirmation(reply, now)

    def test_all_split_positions_and_single_byte_delivery(self):
        for sample in SAMPLES:
            for cut in range(1, len(sample)):
                parser = FrameParser()
                self.assertEqual(parser.feed(sample[:cut]), [])
                self.assertEqual(parser.feed(sample[cut:]), [("frame", sample)])
            parser = FrameParser()
            parsed = []
            for byte in sample:
                parsed.extend(parser.feed(bytes([byte])))
            self.assertEqual(parsed, [("frame", sample)])

    def test_concatenation_maximum_payload_and_internal_delimiters(self):
        payload = (b"@@##\x00\xff" * 171)[:1024]
        sample = SAMPLES[0]
        long_frame = encode_frame(123, sample[4:6], sample[6:12], sample[12:18], sample[18:24], 2, payload)
        frames = SAMPLES * 5 + [long_frame]
        parser = FrameParser()
        self.assertEqual(parser.feed(b"".join(frames)), [("frame", f) for f in frames])
        self.assertEqual(parser.buffer, b"")
        self.assertEqual(decode_frame(long_frame)["application_hex"], payload.hex())

    def test_corruption_noise_resynchronization_and_partial_tail(self):
        sample = SAMPLES[0]
        bad_checksum = bytearray(sample)
        bad_checksum[-3] ^= 1
        bad_end = sample[:-2] + b"!!"
        bad_length = bytearray(sample[:27])
        bad_length[24:26] = b"\xff\xff"
        parser = FrameParser()
        result = parser.feed(b"noise" + bytes(bad_checksum) + bad_end + bytes(bad_length) + sample + b"@")
        self.assertEqual([value for kind, value in result if kind == "frame"], [sample])
        self.assertGreaterEqual(sum(kind == "invalid" for kind, _ in result), 3)
        self.assertEqual(parser.buffer, b"@")
        self.assertEqual(parser.feed(sample[1:]), [("frame", sample)])

    def test_unknown_version_command_and_bounded_duplicates(self):
        sample = SAMPLES[0]
        for version, command in [(b"\x02\x02", 2), (b"\x01\x02", 3), (b"\x01\x02", 1)]:
            frame = encode_frame(1, version, sample[6:12], sample[12:18], sample[18:24], command)
            with self.assertRaises(ValueError):
                make_confirmation(frame)
        cache = RecentFrames(limit=2)
        self.assertFalse(cache.observe(SAMPLES[0]))
        self.assertTrue(cache.observe(SAMPLES[0]))
        cache.observe(SAMPLES[1])
        cache.observe(SAMPLES[2])
        self.assertFalse(cache.observe(SAMPLES[0]))

    def test_partial_socket_writes_log_actual_bytes_and_count_only_complete_ack(self):
        reply = make_confirmation(SAMPLES[0])
        client = Mock()
        client.send.side_effect = [5, BlockingIOError(), len(reply) - 5]
        receiver = Mock()
        receiver.confirmations = receiver.sent_total = 0
        events = []
        receiver.event.side_effect = lambda kind, **fields: events.append((kind, fields))
        import hashlib
        state = dict(connection="test", pending=deque([dict(data=reply, offset=0, sequence=55)]),
                     pending_bytes=len(reply), tx_raw=io.BytesIO(), tx_digest=hashlib.sha256(), tx_offset=0)
        Capture.send_pending(receiver, client, state)
        self.assertEqual(receiver.confirmations, 0)
        self.assertEqual(state["tx_raw"].getvalue(), reply[:5])
        Capture.send_pending(receiver, client, state)
        self.assertEqual(state["tx_offset"], 5)
        Capture.send_pending(receiver, client, state)
        self.assertEqual(receiver.confirmations, 1)
        self.assertEqual(receiver.sent_total, 30)
        self.assertEqual(state["pending_bytes"], 0)
        self.assertEqual(state["tx_raw"].getvalue(), reply)
        self.assertEqual(b"".join(bytes.fromhex(e["hex"]) for k, e in events if k == "sent"), reply)


if __name__ == "__main__":
    unittest.main()
