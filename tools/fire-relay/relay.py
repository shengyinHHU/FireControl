"""FireControl Windows relay. Standard library only; frozen EXE needs no Python/JDK."""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import hmac
import ipaddress
import json
import logging
from logging.handlers import RotatingFileHandler
import os
from pathlib import Path
import re
import select
import signal
import socket
import ssl
import struct
import subprocess
import sys
import threading
import time
import uuid

VERSION = "1.0.0"
MAGIC = b"FCR1"
LOG = logging.getLogger("fire-relay")


def app_directory() -> Path:
    return Path(sys.executable if getattr(sys, "frozen", False) else __file__).resolve().parent


def read_exact(stream, size: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < size:
        block = stream.recv(size - len(chunks))
        if not block:
            raise ConnectionError("接收端已关闭连接")
        chunks.extend(block)
    return bytes(chunks)


def text_field(text: str) -> bytes:
    encoded = text.encode("utf-8")
    return struct.pack("!H", len(encoded)) + encoded


@dataclass(frozen=True)
class Config:
    path: Path
    node_id: str
    listen_address: str
    listen_port: int
    target_address: str
    target_port: int
    fingerprint: str
    secret: bytes
    max_connections: int
    connect_timeout: float
    idle_seconds: int
    reconnect_seconds: float
    log_directory: Path
    ssh: dict

    @staticmethod
    def load(path: Path) -> "Config":
        path = path.resolve()
        raw = json.loads(path.read_text(encoding="utf-8-sig"))
        if not isinstance(raw, dict):
            raise ValueError("config.json 顶层必须为对象")
        allowed = {"node_id", "listen", "target", "max_connections", "connect_timeout_seconds",
                   "idle_seconds", "reconnect_seconds", "log_directory", "ssh"}
        if set(raw) - allowed:
            raise ValueError("不认识的配置项：" + ", ".join(sorted(set(raw) - allowed)))
        node = raw.get("node_id", "site-01")
        if not isinstance(node, str) or not re.fullmatch(r"[A-Za-z0-9_.-]{1,64}", node):
            raise ValueError("node_id 只能使用字母、数字、点、横线、下划线，最长64字符")
        listen, target = raw["listen"], raw["target"]
        if set(listen) - {"address", "port"} or set(target) - {"address", "port", "certificate_sha256", "secret_file"}:
            raise ValueError("listen / target 存在未知配置项")
        ipaddress.IPv4Address(listen["address"])
        if not isinstance(target["address"], str) or not target["address"].strip():
            raise ValueError("target.address 不能为空")
        for value in (listen["port"], target["port"]):
            if type(value) is not int or not 1 <= value <= 65535:
                raise ValueError("端口必须为1到65535之间的整数")
        fingerprint = target["certificate_sha256"].replace(":", "").lower()
        if not re.fullmatch(r"[a-f0-9]{64}", fingerprint):
            raise ValueError("certificate_sha256 必须为接收端证书的64位SHA256指纹")
        secret_path = path.parent / target["secret_file"]
        secret = secret_path.read_text(encoding="utf-8").strip().encode("utf-8")
        if not 32 <= len(secret) <= 1024:
            raise ValueError("密钥文件必须包含32到1024字节的密钥")
        maximum = raw.get("max_connections", 64)
        idle = raw.get("idle_seconds", 600)
        connect = raw.get("connect_timeout_seconds", 5)
        retry = raw.get("reconnect_seconds", 5)
        if type(maximum) is not int or not 1 <= maximum <= 256:
            raise ValueError("max_connections 必须在1到256之间")
        if type(idle) is not int or not 5 <= idle <= 86400:
            raise ValueError("idle_seconds 必须在5到86400之间")
        if isinstance(connect, bool) or not isinstance(connect, (int, float)) or not 1 <= connect <= 30:
            raise ValueError("connect_timeout_seconds 必须在1到30之间")
        if isinstance(retry, bool) or not isinstance(retry, (int, float)) or not 1 <= retry <= 300:
            raise ValueError("reconnect_seconds 必须在1到300之间")
        ssh = dict(raw.get("ssh", {}))
        if set(ssh) - {"enabled", "host", "port", "username", "private_key_file", "known_hosts_file",
                       "local_port", "remote_address", "remote_port"}:
            raise ValueError("ssh 存在未知配置项")
        if type(ssh.get("enabled", False)) is not bool:
            raise ValueError("ssh.enabled 必须为 true 或 false")
        if ssh.get("enabled", False):
            if not re.fullmatch(r"[A-Za-z0-9_.-]+", ssh.get("username", "")):
                raise ValueError("请配置有效的SSH username")
            host = ssh.get("host", "")
            if not isinstance(host, str) or not re.fullmatch(r"[A-Za-z0-9_.-]+", host) or host.startswith("-"):
                raise ValueError("请配置有效的SSH host")
            ipaddress.IPv4Address(ssh.get("remote_address", "127.0.0.1"))
            for name, default in (("port", 18881), ("local_port", 22100), ("remote_port", target["port"])):
                value = ssh.get(name, default)
                if type(value) is not int or not 1 <= value <= 65535:
                    raise ValueError("ssh." + name + " 必须为有效端口")
                ssh[name] = value
            for name in ("private_key_file", "known_hosts_file"):
                ssh[name] = str((path.parent / ssh[name]).resolve())
                if not Path(ssh[name]).is_file():
                    raise ValueError("找不到 SSH 文件：" + name)
        return Config(path, node, listen["address"], listen["port"], target["address"].strip(),
                      target["port"], fingerprint, secret, maximum, float(connect), idle, float(retry),
                      (path.parent / raw.get("log_directory", "logs")).resolve(), ssh)


def connect_target(config: Config, source_ip: str = "", source_port: int = 0) -> ssl.SSLSocket:
    """Pin the server certificate before sending authentication or device bytes."""
    address = (("127.0.0.1", config.ssh["local_port"]) if config.ssh.get("enabled")
               else (config.target_address, config.target_port))
    raw = socket.create_connection(address, timeout=config.connect_timeout)
    try:
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
        context.minimum_version = ssl.TLSVersion.TLSv1_2
        context.check_hostname = False
        # Explicit certificate pinning below replaces CA/hostname validation; never optional.
        context.verify_mode = ssl.CERT_NONE
        tls = context.wrap_socket(raw, server_hostname=config.target_address)
    except BaseException:
        raw.close()
        raise
    try:
        actual = hashlib.sha256(tls.getpeercert(binary_form=True)).hexdigest()
        if not hmac.compare_digest(actual, config.fingerprint):
            raise ssl.SSLError("接收端证书指纹不匹配，已拒绝发送数据")
        challenge = read_exact(tls, 36)
        if challenge[:4] != MAGIC:
            raise ConnectionError("目标不是兼容的FireControl转发接收端")
        kind = 1 if source_ip else 0
        header = (bytes([kind]) + text_field(config.node_id) + text_field(source_ip)
                  + struct.pack("!H", source_port) + text_field(str(uuid.uuid4())))
        signature = hmac.new(config.secret, challenge + header, hashlib.sha256).digest()
        tls.sendall(header + signature)
        if read_exact(tls, 1) != b"\x01":
            raise PermissionError("接收端拒绝了节点身份或设备来源")
        return tls
    except BaseException:
        tls.close()
        raise


class SshTunnel:
    def __init__(self, config: Config, stop: threading.Event):
        self.config, self.stop = config, stop
        self.process = None

    def run(self):
        cfg = self.config.ssh
        known_hosts = cfg["known_hosts_file"].replace("\\", "/")
        while not self.stop.is_set():
            args = ["ssh.exe" if os.name == "nt" else "ssh", "-N", "-T", "-p", str(cfg["port"]),
                    "-i", cfg["private_key_file"], "-o", "BatchMode=yes", "-o", "IdentitiesOnly=yes",
                    "-o", "StrictHostKeyChecking=yes", "-o", 'UserKnownHostsFile="' + known_hosts + '"',
                    "-o", "ExitOnForwardFailure=yes", "-o", "ServerAliveInterval=15",
                    "-o", "ServerAliveCountMax=3", "-o", "ConnectTimeout=5", "-L",
                    f"127.0.0.1:{cfg['local_port']}:{cfg.get('remote_address', '127.0.0.1')}:{cfg['remote_port']}",
                    cfg["username"] + "@" + cfg["host"]]
            try:
                LOG.info("正在建立SSH通道 %s:%s", cfg["host"], cfg["port"])
                self.process = subprocess.Popen(args, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                                                stderr=subprocess.PIPE, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
                def errors(process):
                    for line in process.stderr:
                        LOG.warning("SSH: %s", line.decode("utf-8", errors="replace").strip()[:500])
                threading.Thread(target=errors, args=(self.process,), daemon=True).start()
                while self.process.poll() is None and not self.stop.wait(.5):
                    pass
                if self.process.poll() is None:
                    self.process.terminate()
                self.process.wait(timeout=5)
            except (OSError, subprocess.SubprocessError) as error:
                LOG.warning("SSH通道未建立：%s", error)
            finally:
                if self.process and self.process.poll() is None:
                    self.process.kill()
                self.process = None
            self.stop.wait(self.config.reconnect_seconds)


class Relay:
    def __init__(self, config: Config):
        self.config = config
        self.stop_event = threading.Event()
        self.lock = threading.Lock()
        self.slots = threading.BoundedSemaphore(config.max_connections)
        self.sockets: set[socket.socket] = set()
        self.threads: set[threading.Thread] = set()
        self.listener = None
        self.active = 0
        self.uploaded = 0
        self.downloaded = 0

    def health(self):
        last = None
        while not self.stop_event.is_set():
            state, reason = True, ""
            try:
                with connect_target(self.config):
                    pass
            except Exception as error:
                state, reason = False, str(error)
            if state != last:
                LOG.info("接收端认证探测成功" if state else "接收端暂不可用：%s；设备连接失败时将关闭，等待设备重连", *([] if state else [reason]))
                last = state
            self.stop_event.wait(self.config.reconnect_seconds)

    def start(self):
        listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            if os.name == "nt":
                listener.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
            listener.bind((self.config.listen_address, self.config.listen_port))
            listener.listen(min(self.config.max_connections, 64))
            listener.settimeout(1)
        except BaseException:
            listener.close()
            raise
        self.listener = listener
        if self.config.ssh.get("enabled"):
            self.launch(SshTunnel(self.config, self.stop_event).run)
        self.launch(self.health)
        LOG.info("FireRelay %s 已启动：%s:%s → %s:%s，节点=%s", VERSION, self.config.listen_address,
                 self.config.listen_port, self.config.target_address, self.config.target_port, self.config.node_id)
        try:
            while not self.stop_event.is_set():
                try:
                    device, peer = listener.accept()
                except socket.timeout:
                    continue
                except OSError:
                    if self.stop_event.is_set():
                        break
                    raise
                if not self.slots.acquire(blocking=False):
                    LOG.warning("连接已满，拒绝设备 %s", peer[0])
                    device.close()
                    continue
                with self.lock:
                    self.sockets.add(device)
                self.launch(lambda sock=device, address=peer: self.session(sock, address))
        finally:
            self.close()

    def launch(self, target):
        def task():
            try:
                target()
            finally:
                with self.lock:
                    self.threads.discard(threading.current_thread())
        thread = threading.Thread(target=task, daemon=True)
        with self.lock:
            self.threads.add(thread)
        thread.start()

    def session(self, device, peer):
        target = None
        counted = False
        totals = [0, 0]
        try:
            device.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            target = connect_target(self.config, peer[0], peer[1])
            target.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            with self.lock:
                self.sockets.add(target)
                self.active += 1
                counted = True
            LOG.info("设备 %s:%s 已接入，当前设备连接=%s", peer[0], peer[1], self.active)

            self.pump(device, target, totals)
        except Exception as error:
            LOG.warning("设备 %s:%s 暂未接入：%s；等待设备重新连接", peer[0], peer[1], error)
        finally:
            for stream in (device, target):
                if stream is not None:
                    try:
                        stream.shutdown(socket.SHUT_RDWR)
                    except OSError:
                        pass
                    stream.close()
            with self.lock:
                self.sockets.discard(device)
                self.sockets.discard(target)
                if counted:
                    self.active -= 1
            self.slots.release()
            LOG.info("设备 %s 会话结束，已提交上行=%s字节，下行=%s字节", peer[0], totals[0], totals[1])

    def pump(self, device, target, totals):
        # One thread owns each TLS object. Bounded queues provide backpressure without SSL races.
        device.setblocking(False)
        target.setblocking(False)
        upload, download = bytearray(), bytearray()
        write_job = None
        read_wants_write = False
        write_wants_read = False
        target_eof = False
        device_eof = False
        eof_deadline = None
        last_activity = time.monotonic()
        limit = 65536
        try:
            while not self.stop_event.is_set():
                if time.monotonic() - last_activity > self.config.idle_seconds:
                    break
                if target_eof and not download:
                    break
                if eof_deadline is not None and time.monotonic() > eof_deadline:
                    break
                reads = ([device] if not device_eof and not target_eof and len(upload) < limit else [])
                if not target_eof and (len(download) < limit or write_job is not None):
                    reads.append(target)
                writes = ([device] if download else [])
                if not target_eof and ((upload and not write_wants_read) or read_wants_write):
                    writes.append(target)
                pending = not target_eof and len(download) < limit and target.pending() > 0
                readable, writable, _ = select.select(reads, writes, [], 0 if pending else .5)
                if device in readable:
                    try:
                        block = device.recv(min(8192, limit - len(upload)))
                    except BlockingIOError:
                        block = None
                    if block == b"":
                        # A device may half-close its send side and still await confirmations.
                        device_eof = True
                        eof_deadline = time.monotonic() + self.config.connect_timeout
                    if block:
                        upload.extend(block); last_activity = time.monotonic()
                if not target_eof and len(download) < limit and (pending or target in readable or (read_wants_write and target in writable)):
                    try:
                        block = target.recv(min(8192, limit - len(download)))
                        read_wants_write = False
                    except ssl.SSLWantReadError:
                        block = None; read_wants_write = False
                    except ssl.SSLWantWriteError:
                        block = None; read_wants_write = True
                    if block == b"":
                        target_eof = True  # Drain confirmations already read before closing the device.
                    if block:
                        download.extend(block); last_activity = time.monotonic()
                if not target_eof and upload and (target in writable or target in readable):
                    if write_job is None:
                        write_job = bytes(upload[:8192])
                    try:
                        sent = target.send(write_job)
                    except ssl.SSLWantReadError:
                        write_wants_read = True
                    except ssl.SSLWantWriteError:
                        write_wants_read = False
                    else:
                        if not sent:
                            break
                        del upload[:sent]; write_job = None; write_wants_read = False
                        totals[0] += sent; last_activity = time.monotonic()
                        with self.lock:
                            self.uploaded += sent
                if download and device in writable:
                    try:
                        sent = device.send(download)
                    except BlockingIOError:
                        pass
                    else:
                        if not sent:
                            break
                        del download[:sent]; totals[1] += sent; last_activity = time.monotonic()
                        with self.lock:
                            self.downloaded += sent
        finally:
            if upload or download:
                LOG.warning("会话关闭，尚未转发的有限缓存：上行=%s字节，下行=%s字节；不跨连接自动重放",
                            len(upload), len(download))

    def close(self):
        self.stop_event.set()
        if self.listener:
            self.listener.close()
        with self.lock:
            streams = list(self.sockets)
            threads = list(self.threads)
        for stream in streams:
            try:
                stream.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            stream.close()
        deadline = time.monotonic() + 7
        for thread in threads:
            if thread is not threading.current_thread():
                thread.join(timeout=max(0, deadline - time.monotonic()))


def configure_logging(config: Config, headless: bool):
    config.log_directory.mkdir(parents=True, exist_ok=True)
    handlers = [RotatingFileHandler(config.log_directory / "relay.log", maxBytes=5 * 1024 * 1024,
                                   backupCount=5, encoding="utf-8")]
    if not headless:
        handlers.append(logging.StreamHandler())
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s", handlers=handlers)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="FireControl 用传设备加密转发程序")
    parser.add_argument("--config", type=Path, default=app_directory() / "config.json")
    parser.add_argument("--check-config", action="store_true")
    parser.add_argument("--probe", action="store_true", help="只测试接收端认证，不监听设备")
    parser.add_argument("--headless", action="store_true")
    parser.add_argument("--version", action="version", version="FireRelay " + VERSION)
    args = parser.parse_args(argv)
    try:
        config = Config.load(args.config)
        if args.check_config:
            print(f"CONFIG_OK node={config.node_id} listen={config.listen_address}:{config.listen_port} "
                  f"target={config.target_address}:{config.target_port} ssh={config.ssh.get('enabled', False)}")
            return 0
        configure_logging(config, args.headless)
        if args.probe:
            if config.ssh.get("enabled"):
                raise ValueError("SSH模式请先启动程序，由后台自动建立隧道和探测；--probe 用于直接TLS模式")
            with connect_target(config):
                LOG.info("PROBE_OK 接收端证书和节点身份认证成功")
            return 0
        app = Relay(config)
        def stop(signum, frame):
            app.stop_event.set()
        signal.signal(signal.SIGINT, stop)
        if hasattr(signal, "SIGTERM"):
            signal.signal(signal.SIGTERM, stop)
        try:
            app.start()
        finally:
            app.close()
        return 0
    except Exception as error:
        LOG.error("启动/连接失败：%s", error)
        if not LOG.handlers and not logging.getLogger().handlers:
            print("启动失败：" + str(error), file=sys.stderr)
        if getattr(sys, "frozen", False) and not args.headless and not args.check_config and not args.probe:
            try:
                input("请检查配置文件或网络；按回车关闭窗口。")
            except (EOFError, OSError):
                pass
        return 1


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
