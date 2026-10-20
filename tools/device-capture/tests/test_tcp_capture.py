import hashlib
import json
from pathlib import Path
import queue
import socket
import subprocess
import sys
import tempfile
import threading
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "tcp_capture.py"


class CaptureTests(unittest.TestCase):
    def launch(self, directory, extra=()):
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", 0))
            port = probe.getsockname()[1]
        process = subprocess.Popen(
            [sys.executable, str(SCRIPT), "--host", "127.0.0.1",
             "--port", str(port), "--duration", "2", "--output", str(directory), *extra],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        self.addCleanup(self.cleanup_process, process)
        lines = queue.Queue()
        threading.Thread(target=lambda: lines.put(process.stdout.readline()), daemon=True).start()
        try:
            first = lines.get(timeout=5)
        except queue.Empty:
            self.fail("Collector did not start within 5 seconds")
        self.assertIn("Listening on", first)
        return process, port

    @staticmethod
    def cleanup_process(process):
        if process.poll() is None:
            process.kill()
        process.communicate(timeout=5)

    def finish(self, process, directory):
        out, err = process.communicate(timeout=6)
        self.assertEqual(process.returncode, 0, out + err)
        summary = json.loads((directory / "summary.json").read_text(encoding="utf-8"))
        events = [json.loads(line) for line in (directory / "events.jsonl").read_text(encoding="utf-8").splitlines()]
        return summary, events

    def test_binary_streams_reconnect_no_ack_and_duration_stop(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "capture"
            process, port = self.launch(directory)
            payloads = [b"\x00\xff@@\r\n" + bytes(range(256)) * 5, b"second connection\x00\xfe"]
            for payload in payloads:
                with socket.create_connection(("127.0.0.1", port), timeout=1) as client:
                    client.sendall(payload[:3])
                    client.sendall(payload[3:])
                    client.settimeout(0.1)
                    with self.assertRaises(socket.timeout):
                        client.recv(1)
            summary, events = self.finish(process, directory)
            self.assertEqual(summary["reason"], "duration_limit")
            self.assertEqual(summary["connections"], 2)
            self.assertEqual(summary["total_bytes"], sum(map(len, payloads)))
            for index, payload in enumerate(payloads, 1):
                key = f"connection_{index:04d}"
                self.assertEqual((directory / (key + ".bin")).read_bytes(), payload)
                received = [e for e in events if e["event"] == "received" and e["connection"] == key]
                offset = 0
                for event in received:
                    self.assertEqual(event["offset"], offset)
                    self.assertEqual(len(bytes.fromhex(event["hex"])), event["length"])
                    self.assertIn("T", event["time"])
                    offset += event["length"]
                self.assertEqual(b"".join(bytes.fromhex(e["hex"]) for e in received), payload)
                closed = next(e for e in events if e["event"] == "disconnected" and e["connection"] == key)
                self.assertEqual(closed["sha256"], hashlib.sha256(payload).hexdigest())

    def test_byte_limit_stops_without_overrun(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "capture"
            # 1 MiB / 65536 = 16 bytes.
            process, port = self.launch(directory, ["--max-mb", str(1 / 65536)])
            with socket.create_connection(("127.0.0.1", port), timeout=1) as client:
                client.sendall(b"x" * 100)
            summary, _ = self.finish(process, directory)
            self.assertEqual(summary["reason"], "byte_limit")
            self.assertEqual(summary["total_bytes"], 16)
            self.assertEqual((directory / "connection_0001.bin").read_bytes(), b"x" * 16)

    def test_occupied_port_and_existing_output_fail_cleanly(self):
        with tempfile.TemporaryDirectory() as temp, socket.socket() as occupied:
            occupied.bind(("127.0.0.1", 0))
            occupied.listen()
            output = Path(temp) / "capture"
            command = [sys.executable, str(SCRIPT), "--host", "127.0.0.1",
                       "--port", str(occupied.getsockname()[1]), "--output", str(output)]
            result = subprocess.run(command, capture_output=True, text=True, timeout=5)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(json.loads((output / "summary.json").read_text())["reason"], "error")
            before = (output / "events.jsonl").read_bytes()
            result = subprocess.run(command, capture_output=True, text=True, timeout=5)
            self.assertEqual(result.returncode, 1)
            self.assertEqual((output / "events.jsonl").read_bytes(), before)



    def test_default_confirmation_split_duplicate_corruption_and_no_ack_loop(self):
        import sys
        sys.path.insert(0, str(SCRIPT.parent))
        from gb26875 import decode_frame, encode_frame
        sample = bytes.fromhex("404037000102380813090311c80000000000640000000000000002d82323")
        command3 = encode_frame(70, sample[4:6], sample[6:12], sample[12:18], sample[18:24], 3)
        unsupported = encode_frame(71, b"\x02\x02", sample[6:12], sample[12:18], sample[18:24], 2)
        broken = bytearray(sample)
        broken[-3] ^= 1
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "capture"
            process, port = self.launch(directory)
            with socket.create_connection(("127.0.0.1", port), timeout=1) as client:
                client.sendall(sample[:10])
                client.settimeout(0.1)
                with self.assertRaises(socket.timeout):
                    client.recv(1)
                remainder = sample[10:] + sample + command3 + unsupported + bytes(broken) + sample
                client.sendall(remainder)
                client.settimeout(1)
                replies = bytearray()
                while len(replies) < 90:
                    chunk = client.recv(90 - len(replies))
                    self.assertTrue(chunk)
                    replies.extend(chunk)
                client.settimeout(0.1)
                with self.assertRaises(socket.timeout):
                    client.recv(1)
            summary, events = self.finish(process, directory)
            self.assertEqual(summary["confirmations_sent"], 3)
            self.assertEqual(summary["valid_frames"], 5)
            self.assertGreaterEqual(summary["invalid_frames"], 1)
            for start in range(0, 90, 30):
                info = decode_frame(replies[start:start + 30])
                self.assertEqual(info["command"], 3)
                self.assertEqual(info["sequence"], 55)
                self.assertEqual(info["source_address"], 100)
                self.assertEqual(info["destination_address"], 200)
            self.assertEqual((directory / "connection_0001.tx.bin").read_bytes(), replies)
            self.assertEqual((directory / "connection_0001.bin").read_bytes(), sample[:10] + remainder)
            sent_events = [e for e in events if e["event"] == "sent"]
            self.assertEqual(b"".join(bytes.fromhex(e["hex"]) for e in sent_events), replies)
            self.assertEqual(sum(e.get("duplicate", False) for e in events if e["event"] == "frame_received"), 2)
            closed = next(e for e in events if e["event"] == "disconnected")
            self.assertEqual(closed["sent_sha256"], hashlib.sha256(replies).hexdigest())

    def test_receive_only_valid_frame_no_confirmation_and_incomplete_tail(self):
        sample = bytes.fromhex("404037000102380813090311c80000000000640000000000000002d82323")
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "capture"
            process, port = self.launch(directory, ["--receive-only"])
            with socket.create_connection(("127.0.0.1", port), timeout=1) as client:
                client.sendall(sample + sample[:11])
                client.settimeout(0.1)
                with self.assertRaises(socket.timeout):
                    client.recv(1)
            summary, events = self.finish(process, directory)
            self.assertTrue(summary["receive_only"])
            self.assertEqual(summary["confirmations_sent"], 0)
            self.assertEqual(summary["valid_frames"], 1)
            self.assertEqual((directory / "connection_0001.tx.bin").read_bytes(), b"")
            incomplete = next(e for e in events if e["event"] == "incomplete_frame")
            self.assertEqual(bytes.fromhex(incomplete["hex"]), sample[:11])


if __name__ == "__main__":
    unittest.main()
