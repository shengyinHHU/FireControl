"""GB/T 26875.3-2011 framing and transport confirmations; no alarm interpretation."""
from collections import OrderedDict
import datetime as dt
import hashlib

MAX_ADU = 1024


def encode_frame(sequence, version, time_bytes, source, destination, command, payload=b""):
    if len(version) != 2 or len(time_bytes) != 6 or len(source) != 6 or len(destination) != 6:
        raise ValueError("Invalid fixed field length")
    if len(payload) > MAX_ADU:
        raise ValueError("Application data exceeds 1024 bytes")
    body = (sequence.to_bytes(2, "little") + version + time_bytes + source + destination
            + len(payload).to_bytes(2, "little") + bytes([command]) + payload)
    return b"@@" + body + bytes([sum(body) & 255]) + b"##"


def decode_frame(frame):
    if len(frame) < 30 or frame[:2] != b"@@" or frame[-2:] != b"##":
        raise ValueError("Invalid frame delimiters or minimum length")
    size = int.from_bytes(frame[24:26], "little")
    if size > MAX_ADU or len(frame) != 30 + size:
        raise ValueError("Invalid application data length")
    if sum(frame[2:-3]) & 255 != frame[-3]:
        raise ValueError("Checksum mismatch")
    raw_time = list(frame[6:12])
    try:
        second, minute, hour, day, month, year = raw_time
        device_time = dt.datetime(2000 + year, month, day, hour, minute, second).isoformat()
    except ValueError:
        device_time = None
    return dict(sequence=int.from_bytes(frame[2:4], "little"),
                version=list(frame[4:6]), device_time=device_time,
                device_time_bytes=raw_time,
                source_address_hex=frame[12:18].hex(),
                destination_address_hex=frame[18:24].hex(),
                source_address=int.from_bytes(frame[12:18], "little"),
                destination_address=int.from_bytes(frame[18:24], "little"),
                command=frame[26], application_length=size,
                application_hex=frame[27:-3].hex())


def make_confirmation(frame, now=None):
    info = decode_frame(frame)
    if info["command"] != 2 or info["version"][0] != 1:
        raise ValueError("Only version-1 send-data frames can be confirmed")
    now = now or dt.datetime.now()
    if not 2000 <= now.year <= 2099:
        raise ValueError("Local clock cannot be represented in the protocol")
    tag = bytes([now.second, now.minute, now.hour, now.day, now.month, now.year - 2000])
    return encode_frame(info["sequence"], frame[4:6], tag, frame[18:24], frame[12:18], 3)


class FrameParser:
    def __init__(self):
        self.buffer = bytearray()

    def feed(self, data):
        self.buffer.extend(data)
        result = []
        while self.buffer:
            start = self.buffer.find(b"@@")
            if start < 0:
                keep = 1 if self.buffer[-1:] == b"@" else 0
                length = len(self.buffer) - keep
                if length:
                    result.append(("discarded", dict(reason="missing_start", hex=bytes(self.buffer[:length]).hex())))
                    del self.buffer[:length]
                break
            if start:
                result.append(("discarded", dict(reason="before_start", hex=bytes(self.buffer[:start]).hex())))
                del self.buffer[:start]
            if len(self.buffer) < 27:
                break
            size = int.from_bytes(self.buffer[24:26], "little")
            if size > MAX_ADU:
                result.append(("invalid", dict(reason="length_exceeds_1024", hex=bytes(self.buffer[:27]).hex())))
                del self.buffer[0]
                continue
            length = 30 + size
            if len(self.buffer) < length:
                break
            candidate = bytes(self.buffer[:length])
            try:
                decode_frame(candidate)
            except ValueError as exc:
                result.append(("invalid", dict(reason=str(exc), hex=candidate.hex())))
                del self.buffer[0]
                continue
            del self.buffer[:length]
            result.append(("frame", candidate))
        return result


class RecentFrames:
    """Bounded observation cache. Duplicates are logged and still acknowledged."""
    def __init__(self, limit=4096):
        self.limit = limit
        self.frames = OrderedDict()

    def observe(self, frame):
        key = hashlib.sha256(frame).hexdigest()
        duplicate = key in self.frames
        self.frames[key] = True
        self.frames.move_to_end(key)
        if len(self.frames) > self.limit:
            self.frames.popitem(last=False)
        return duplicate
