#!/usr/bin/env python3
"""TCP diagnostic receiver with GB/T 26875.3-2011 frame confirmations."""
import argparse
from collections import deque
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import selectors
import socket
import sys
import time

from gb26875 import FrameParser, RecentFrames, decode_frame, make_confirmation

MAX_PENDING_SEND = 65536


def timestamp():
    return dt.datetime.now(dt.timezone.utc).astimezone().isoformat(timespec="milliseconds")


class Capture:
    def __init__(self, output, host, port, duration, max_bytes, receive_only=False):
        self.output = Path(output)
        self.output.mkdir(parents=True, exist_ok=False)
        self.log = (self.output / "events.jsonl").open("x", encoding="utf-8", buffering=1)
        self.selector = selectors.DefaultSelector()
        self.clients = {}
        self.counter = self.total = self.sent_total = self.valid_frames = self.invalid_frames = self.confirmations = 0
        self.recent = RecentFrames()
        self.receive_only = receive_only
        self.max_bytes = max_bytes
        self.duration = duration
        self.started = time.monotonic()
        self.reason = "not_started"
        self.listening = False
        self.host = host
        self.port = port

    def event(self, kind, **fields):
        self.log.write(json.dumps(dict(time=timestamp(), event=kind, **fields), ensure_ascii=False) + "\n")

    def accept(self, listener):
        client, peer = listener.accept()
        client.setblocking(False)
        self.counter += 1
        key = f"connection_{self.counter:04d}"
        filename = key + ".bin"
        opened = []
        try:
            raw = (self.output / filename).open("xb")
            opened.append(raw)
            tx_raw = (self.output / (key + ".tx.bin")).open("xb")
        except Exception:
            for handle in opened:
                handle.close()
            client.close()
            raise
        state = dict(connection=key, peer_ip=peer[0], peer_port=peer[1],
                     file=filename, raw=raw, offset=0, digest=hashlib.sha256(),
                     tx_raw=tx_raw, tx_offset=0, tx_digest=hashlib.sha256(),
                     parser=FrameParser(), pending=deque(), pending_bytes=0)
        self.clients[client] = state
        self.selector.register(client, selectors.EVENT_READ, state)
        self.event("connected", connection=key, peer_ip=peer[0], peer_port=peer[1],
                   file=filename, tx_file=key + ".tx.bin")
        print(f"[{timestamp()}] Connected: {peer[0]}:{peer[1]} -> {key}", flush=True)

    def close_client(self, client, reason):
        state = self.clients.pop(client)
        self.selector.unregister(client)
        client.close()
        state["raw"].close()
        state["tx_raw"].close()
        if state["parser"].buffer:
            self.event("incomplete_frame", connection=state["connection"],
                       length=len(state["parser"].buffer), hex=state["parser"].buffer.hex())
        if state["pending_bytes"]:
            self.event("unsent_confirmations", connection=state["connection"],
                       bytes=state["pending_bytes"], count=len(state["pending"]))
        self.event("disconnected", connection=state["connection"], reason=reason,
                   bytes=state["offset"], sha256=state["digest"].hexdigest(),
                   sent_bytes=state["tx_offset"], sent_sha256=state["tx_digest"].hexdigest())
        print(f"[{timestamp()}] Disconnected: {state['connection']}, received={state['offset']}, sent={state['tx_offset']}, {reason}", flush=True)

    def process_frames(self, client, state, data):
        for kind, item in state["parser"].feed(data):
            if kind != "frame":
                self.invalid_frames += kind == "invalid"
                self.event("frame_" + kind, connection=state["connection"], **item)
                continue
            self.valid_frames += 1
            info = decode_frame(item)
            duplicate = self.recent.observe(item)
            self.event("frame_received", connection=state["connection"], hex=item.hex(),
                       duplicate=duplicate, **info)
            print(f"[{timestamp()}] Frame: seq={info['sequence']}, command={info['command']}, application_bytes={info['application_length']}, duplicate={duplicate}", flush=True)
            if self.receive_only:
                continue
            if info["version"][0] != 1 or info["command"] != 2:
                self.event("confirmation_skipped", connection=state["connection"],
                           sequence=info["sequence"], reason="unsupported_version_or_command")
                continue
            # This is a transport receipt for durable raw capture, not alarm acceptance.
            # Every retransmission is confirmed again; command 3 never causes an ACK loop.
            ack = make_confirmation(item)
            if state["pending_bytes"] + len(ack) > MAX_PENDING_SEND:
                self.event("send_queue_limit", connection=state["connection"])
                self.close_client(client, "send_queue_limit")
                return
            self.log.flush()
            os.fsync(self.log.fileno())
            state["pending"].append(dict(data=ack, offset=0, sequence=info["sequence"]))
            state["pending_bytes"] += len(ack)
            self.event("confirmation_queued", connection=state["connection"],
                       sequence=info["sequence"], length=len(ack), hex=ack.hex())
        if client in self.clients and state["pending"]:
            self.selector.modify(client, selectors.EVENT_READ | selectors.EVENT_WRITE, state)

    def receive(self, client, state):
        remaining = self.max_bytes - self.total
        if remaining <= 0:
            self.reason = "byte_limit"
            return
        try:
            data = client.recv(min(65536, remaining))
        except BlockingIOError:
            return
        except OSError as exc:
            self.event("socket_error", connection=state["connection"], error=str(exc))
            self.close_client(client, "socket_error")
            return
        if not data:
            self.close_client(client, "peer_closed")
            return
        offset = state["offset"]
        state["raw"].write(data)
        state["raw"].flush()
        if not self.receive_only:
            os.fsync(state["raw"].fileno())
        state["digest"].update(data)
        state["offset"] += len(data)
        self.total += len(data)
        self.event("received", connection=state["connection"], direction="device_to_server",
                   offset=offset, length=len(data), hex=data.hex())
        print(f"[{timestamp()}] Received: {state['connection']}, offset={offset}, bytes={len(data)}", flush=True)
        self.process_frames(client, state, data)
        if self.total >= self.max_bytes:
            self.reason = "byte_limit"

    def send_pending(self, client, state):
        if not state["pending"]:
            return
        item = state["pending"][0]
        try:
            sent = client.send(item["data"][item["offset"]:])
            if sent == 0:
                raise ConnectionError("Socket sent zero bytes")
        except BlockingIOError:
            return
        except OSError as exc:
            self.event("send_error", connection=state["connection"], error=str(exc))
            self.close_client(client, "send_error")
            return
        actual = item["data"][item["offset"]:item["offset"] + sent]
        state["tx_raw"].write(actual)
        state["tx_raw"].flush()
        state["tx_digest"].update(actual)
        self.event("sent", connection=state["connection"], direction="server_to_device",
                   sequence=item["sequence"], offset=state["tx_offset"], length=sent, hex=actual.hex())
        state["tx_offset"] += sent
        state["pending_bytes"] -= sent
        self.sent_total += sent
        item["offset"] += sent
        if item["offset"] == len(item["data"]):
            self.confirmations += 1
            self.event("confirmation_sent", connection=state["connection"],
                       sequence=item["sequence"], hex=item["data"].hex())
            print(f"[{timestamp()}] ACK sent: {state['connection']}, seq={item['sequence']}", flush=True)
            state["pending"].popleft()
        if not state["pending"]:
            self.selector.modify(client, selectors.EVENT_READ, state)

    def run(self):
        listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        if hasattr(socket, "SO_EXCLUSIVEADDRUSE"):
            listener.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
        try:
            listener.bind((self.host, self.port))
            listener.listen(16)
            listener.setblocking(False)
            self.selector.register(listener, selectors.EVENT_READ, None)
            self.listening = True
            self.reason = "running"
            self.event("started", host=self.host, port=self.port, protocol="TCP",
                       receive_only=self.receive_only, duration_seconds=self.duration, max_bytes=self.max_bytes)
            mode = "receive only" if self.receive_only else "GB26875 frame confirmation enabled"
            print(f"Listening on {self.host}:{self.port} (TCP, {mode})", flush=True)
            print(f"Output: {self.output.resolve()}", flush=True)
            print(f"Stops after {self.duration}s or {self.max_bytes} received bytes. Ctrl+C stops early.", flush=True)
            print("Diagnostic capture only; confirmations do not mean an alarm was handled.", flush=True)
            while self.reason == "running":
                left = self.duration - (time.monotonic() - self.started)
                if left <= 0:
                    self.reason = "duration_limit"
                    break
                for key, mask in self.selector.select(min(0.25, left)):
                    if key.data is None:
                        try:
                            self.accept(listener)
                        except BlockingIOError:
                            pass
                    else:
                        client = key.fileobj
                        if mask & selectors.EVENT_READ:
                            self.receive(client, key.data)
                        if client in self.clients and mask & selectors.EVENT_WRITE and self.reason == "running":
                            self.send_pending(client, key.data)
                    if self.reason != "running":
                        break
        except KeyboardInterrupt:
            self.reason = "user_stopped"
        except Exception as exc:
            self.reason = "error"
            self.event("error", error=str(exc))
            raise
        finally:
            for client in list(self.clients):
                self.close_client(client, "collector_stopped")
            listener.close()
            self.selector.close()
            self.event("stopped", reason=self.reason, total_bytes=self.total, sent_bytes=self.sent_total,
                       connections=self.counter, valid_frames=self.valid_frames,
                       invalid_frames=self.invalid_frames, confirmations_sent=self.confirmations)
            self.log.close()
            summary = dict(started_capture=self.listening, stopped_at=timestamp(), reason=self.reason,
                           total_bytes=self.total, sent_bytes=self.sent_total, connections=self.counter,
                           valid_frames=self.valid_frames, invalid_frames=self.invalid_frames,
                           confirmations_sent=self.confirmations, protocol="TCP",
                           receive_only=self.receive_only, host=self.host, port=self.port,
                           duration_limit_seconds=self.duration, max_bytes=self.max_bytes)
            (self.output / "summary.json").write_text(
                json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print(f"Stopped: {self.reason}; connections={self.counter}, received={self.total}, sent={self.sent_total}, ACKs={self.confirmations}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="10.5.90.50", help="Local IPv4 address to bind")
    parser.add_argument("--port", type=int, default=2000)
    parser.add_argument("--duration", type=float, default=300, help="Maximum capture seconds; default 300")
    parser.add_argument("--max-mb", type=float, default=100, help="Maximum received MiB for all connections")
    parser.add_argument("--receive-only", action="store_true", help="Disable protocol confirmations")
    parser.add_argument("--output", type=Path, help="New directory; existing directories are never overwritten")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535 or not 0 < args.duration <= 86400 or not 0 < args.max_mb <= 1024:
        parser.error("port must be 1..65535; duration must be 0..86400; max-mb must be 0..1024 (exclusive zero)")
    output = args.output or Path(__file__).parent / "captures" / dt.datetime.now().strftime("%Y%m%d_%H%M%S_%f")
    try:
        capture = Capture(output, args.host, args.port, args.duration,
                          max(1, int(args.max_mb * 1024 * 1024)), args.receive_only)
        capture.run()
    except OSError as exc:
        print(f"Capture failed: {exc}", file=sys.stderr)
        print("Check the local IP, port ownership and output directory permissions. Existing output is never overwritten.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
