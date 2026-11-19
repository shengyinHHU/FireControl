"""Cross-language TLS/EXE/GB26875 integration on loopback and an isolated temporary MySQL."""
from __future__ import annotations
from dataclasses import replace
import importlib.util
import json
import os
from pathlib import Path
import queue
import re
import shutil
import socket
import ssl
import struct
import subprocess
import sys
import tempfile
import threading
import time

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import relay
from build import provision

FLAGS = getattr(subprocess, "CREATE_NO_WINDOW", 0)
CHECKS = []
WORK = Path(tempfile.mkdtemp(prefix="fire-relay-verify-"))
MYSQL = Path(r"C:\Program Files\MySQL\MySQL Server 8.0\bin")


def check(value, message):
    if not value:
        raise AssertionError(message)
    CHECKS.append(message)
    print("PASS " + message, flush=True)


def run(args, **kwargs):
    return subprocess.run([str(a) for a in args], creationflags=FLAGS, stdout=subprocess.PIPE,
                          stderr=subprocess.STDOUT, **kwargs)


def port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def frame(sequence=123, event=False):
    base = bytearray.fromhex("404037000102380813090311c80000000000640000000000000002d82323")
    data = bytes([21, 1, 3, 0, 12, 20, 10, 26, 0]) if event else b""
    # Gateway status object is 1 status byte + six timestamp bytes.
    if event:
        data = bytes([21, 1, 3, 0, 12, 20, 10, 11, 26])
    base[2:4] = struct.pack("<H", sequence)
    base[24:26] = struct.pack("<H", len(data))
    result = base[:27] + data + b"\0##"
    result[-3] = sum(result[2:-3]) & 255
    return bytes(result)


def ack(stream, sequence):
    value = relay.read_exact(stream, 30)
    check(value[:2] == b"@@" and value[-2:] == b"##" and value[26] == 3
          and struct.unpack("<H", value[2:4])[0] == sequence and sum(value[2:-3]) & 255 == value[-3],
          "valid ACK sequence=" + str(sequence))


def rejected(config, source="10.5.90.49"):
    try:
        with relay.connect_target(config, source, 35000):
            return False
    except (OSError, ValueError):
        return True


def main():
    bin_mysql = MYSQL / "mysql.exe"
    dbport = port()
    init = run([MYSQL / "mysqld.exe", "--no-defaults", "--initialize-insecure", "--datadir=" + str(WORK / "mysql")], timeout=60)
    if init.returncode:
        raise RuntimeError(init.stdout.decode(errors="replace"))
    db_log = (WORK / "mysql.log").open("wb")
    database = subprocess.Popen([str(MYSQL / "mysqld.exe"), "--no-defaults", "--datadir=" + str(WORK / "mysql"),
                                 "--port=" + str(dbport), "--bind-address=127.0.0.1", "--mysqlx=OFF",
                                 "--skip-log-bin", "--console"], stdout=db_log, stderr=db_log, creationflags=FLAGS)
    mysql = [bin_mysql, "--no-defaults", "--protocol=TCP", "--host=127.0.0.1", "--port=" + str(dbport),
             "--user=root", "--default-character-set=utf8mb4", "--batch", "--skip-column-names"]

    def sql(text):
        result = run(mysql, input=text.encode("utf-8"), timeout=15)
        if result.returncode:
            raise RuntimeError(result.stdout.decode(errors="replace"))
        return result.stdout.decode("utf-8").strip()

    host = None
    app = None
    exe_process = None
    host_files = []
    try:
        for _ in range(100):
            try:
                sql("SELECT 1"); break
            except RuntimeError:
                time.sleep(.2)
        else:
            raise RuntimeError("temporary MySQL startup timed out")
        framework = (ROOT / "SQL/ry_20260417.sql").read_text("utf-8")
        bootstrap = ["CREATE DATABASE dachang CHARACTER SET utf8mb4;USE dachang;"]
        for table in ["sys_dict_type", "sys_dict_data", "sys_menu", "sys_role_menu"]:
            bootstrap.append(re.search(r"create table " + table + r"\s*\([\s\S]*?;", framework, re.I).group(0))
        sql("\n".join(bootstrap))
        for name in ["fire_place_init.sql", "fire_device_access.sql", "fire_gateway_management_upgrade.sql",
                     "fire_gateway_ip_identity_upgrade.sql", "fire_gateway_only_upgrade.sql"]:
            sql((ROOT / "SQL" / name).read_text("utf-8"))
        maven = shutil.which("mvn.cmd") or shutil.which("mvn")
        build = run([maven, "-o", "-X", "-pl", "ruoyi-admin", "-am", "-DskipTests", "package"],
                    cwd=ROOT / "RuoYi-Vue-master", timeout=180)
        (WORK / "maven.log").write_bytes(build.stdout)
        if build.returncode:
            raise RuntimeError("Backend build failed: " + str(WORK / "maven.log"))
        check(True, "backend package BUILD SUCCESS")
        paths = re.findall(r"compilePath = \[([^\r\n]+)\]", build.stdout.decode(errors="replace"))
        cp = os.pathsep.join(paths[-1].split(", "))
        classes = WORK / "classes"; classes.mkdir()
        compiled = run(["javac", "-encoding", "UTF-8", "-cp", cp, "-d", classes,
                        HERE / "tests/FireRelayTestHost.java"], timeout=30)
        if compiled.returncode:
            raise RuntimeError(compiled.stdout.decode(errors="replace"))
        forwarder, private = provision(WORK / "paired", target="127.0.0.1", bind="127.0.0.1")
        config_data = json.loads((forwarder / "config.json").read_text("utf-8"))
        config_data["listen"] = {"address": "127.0.0.1", "port": port()}
        config_data["target"]["port"] = port()
        config_data["idle_seconds"] = 5
        config_data["reconnect_seconds"] = 1
        config_data["log_directory"] = "logs"
        config_path = forwarder / "config.json"
        config_path.write_text(json.dumps(config_data), encoding="utf-8")
        config = relay.Config.load(config_path)
        receiver_port = config.target_port
        direct_port = port()

        def start_host():
            output = (WORK / ("host-" + str(len(host_files)) + ".log")).open("wb")
            host_files.append(output)
            process = subprocess.Popen(["java", "-Dfile.encoding=UTF-8", "-cp", str(classes) + os.pathsep + cp,
                                        "FireRelayTestHost", str(dbport), str(ROOT), str(private), "unused", "unused",
                                        str(receiver_port), str(direct_port)], stdin=subprocess.PIPE,
                                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT, creationflags=FLAGS)
            ready = threading.Event()
            def read():
                for line in process.stdout:
                    output.write(line); output.flush()
                    if line.strip() == b"READY":
                        ready.set()
            threading.Thread(target=read, daemon=True).start()
            if not ready.wait(20):
                process.terminate(); process.wait(timeout=5)
                raise RuntimeError("Java host not ready; logs: " + str(WORK))
            return process

        def stop_host(process):
            process.stdin.write(b"STOP\n"); process.stdin.flush()
            process.wait(timeout=10)

        def messages():
            return int(sql("USE dachang;SELECT COUNT(*) FROM fire_protocol_message;"))

        host = start_host()
        check(True, "real TLS relay and original direct listener started")
        before = messages()
        with relay.connect_target(config):
            pass
        check(messages() == before, "health probe does not create business messages")
        check(rejected(replace(config, secret=b"bad-secret" * 5)), "wrong node secret rejected")
        check(rejected(replace(config, node_id="another-node")), "wrong node identity rejected")
        check(rejected(replace(config, fingerprint="0" * 64)), "wrong server certificate pin rejected")
        check(rejected(config, "10.9.0.1"), "unauthorized reported source subnet rejected")
        check(messages() == before, "rejected authentication cannot write business messages")

        with relay.connect_target(config, "10.5.89.201", 35001) as first, relay.connect_target(config, "10.5.90.49", 35002) as second:
            data = frame(401, True)
            first.sendall(data[:4]); first.sendall(data[4:])
            second.sendall(frame(402, True) + frame(403, True))
            ack(first, 401); ack(second, 402); ack(second, 403)
            identities = sql("USE dachang;SELECT allowed_ip FROM fire_gateway WHERE allowed_ip IN ('10.5.89.201','10.5.90.49') ORDER BY allowed_ip;")
            check(identities.splitlines() == ["10.5.89.201", "10.5.90.49"], "two devices with the same protocol address retain distinct source identities")
            check(int(sql("USE dachang;SELECT COUNT(*) FROM fire_event WHERE gateway_id IN (SELECT gateway_id FROM fire_gateway WHERE allowed_ip IN ('10.5.89.201','10.5.90.49'));")) == 2,
                  "ACK follows committed events and repeated report remains deduplicated")

        # A repeated captured header must not authenticate against a new random challenge.
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT); context.check_hostname = False; context.verify_mode = ssl.CERT_NONE
        header = bytes([0]) + relay.text_field(config.node_id) + relay.text_field("") + b"\0\0" + relay.text_field("12345678-1234-1234-1234-123456789abc")
        with context.wrap_socket(socket.create_connection(("127.0.0.1", receiver_port)), server_hostname="localhost") as capture:
            challenge = relay.read_exact(capture, 36)
            signature = relay.hmac.new(config.secret, challenge + header, relay.hashlib.sha256).digest()
            capture.sendall(header + signature); check(relay.read_exact(capture, 1) == b"\x01", "challenge authentication accepted once")
        with context.wrap_socket(socket.create_connection(("127.0.0.1", receiver_port)), server_hostname="localhost") as replay:
            replay.settimeout(3); challenge2 = relay.read_exact(replay, 36)
            replay.sendall(header + signature)
            try:
                replay_result = replay.recv(1)
            except OSError:
                replay_result = b""
            check(challenge2 != challenge and replay_result != b"\x01", "captured authentication cannot be replayed")

        held = [relay.connect_target(config, "10.5.90.49", 36000 + i) for i in range(4)]
        try:
            check(rejected(config), "receiver enforces active device connection limit")
            with relay.connect_target(config):
                pass
            check(True, "health checks still work at device connection limit")
        finally:
            for connection in held:
                connection.close()
        time.sleep(.3)

        # A disabled device receives no successful acknowledgement through the relay.
        sql("USE dachang;UPDATE fire_gateway SET enabled='1' WHERE allowed_ip='10.5.90.49';")
        with relay.connect_target(config, "10.5.90.49", 37000) as disabled:
            disabled.sendall(frame(404)); disabled.settimeout(.5)
            try:
                value = disabled.recv(30)
            except socket.timeout:
                value = b""
            check(not value, "disabled gateway receives no success ACK")
        sql("USE dachang;UPDATE fire_gateway SET enabled='0' WHERE allowed_ip='10.5.90.49';")

        # Simulate failed raw persistence: connection must close without success ACK or registration.
        sql("USE dachang;RENAME TABLE fire_protocol_message TO fire_protocol_message_saved;")
        try:
            with relay.connect_target(config, "10.5.89.202", 37001) as broken:
                broken.sendall(frame(405)); broken.settimeout(4)
                try:
                    value = broken.recv(30)
                except OSError:
                    value = b""
                check(not value, "database failure never produces a successful ACK")
        finally:
            sql("USE dachang;RENAME TABLE fire_protocol_message_saved TO fire_protocol_message;")
        check(sql("USE dachang;SELECT COUNT(*) FROM fire_gateway WHERE allowed_ip='10.5.89.202';") == "0",
              "failed raw persistence cannot auto-register a device")

        with socket.create_connection(("127.0.0.1", direct_port), timeout=4) as direct:
            direct.sendall(frame(406)); ack(direct, 406)
        check(True, "original direct TCP reception still works")

        app = relay.Relay(config)
        app_thread = threading.Thread(target=app.start, daemon=True); app_thread.start()
        for _ in range(100):
            if app.listener:
                break
            time.sleep(.03)
        check(app.listener is not None, "Python relay listens on configured address")
        with socket.create_connection((config.listen_address, config.listen_port), timeout=4) as device:
            device.settimeout(8)
            # Exercise bounded forwarding/backpressure with substantial noise and split/concatenated frames.
            device.sendall(b"\0" * 180000 + frame(407)[:7])
            device.sendall(frame(407)[7:] + frame(408))
            ack(device, 407); ack(device, 408)
        with socket.create_connection((config.listen_address, config.listen_port), timeout=4) as half_closed:
            half_closed.settimeout(8); half_closed.sendall(frame(412)); half_closed.shutdown(socket.SHUT_WR)
            ack(half_closed, 412)
        check(True, "device send-side half-close still receives pending confirmation")
        time.sleep(.2)
        check(app.uploaded >= 180060 and app.downloaded >= 60, "bidirectional relay handles backpressure, split and concatenated frames")
        stop_host(host); host = None
        with socket.create_connection((config.listen_address, config.listen_port), timeout=4) as disconnected:
            disconnected.settimeout(8); disconnected.sendall(frame(409))
            try:
                value = disconnected.recv(30)
            except OSError:
                value = b""
            check(not value, "upstream outage never fakes device confirmation")
        host = start_host()
        with socket.create_connection((config.listen_address, config.listen_port), timeout=4) as recovered:
            recovered.settimeout(8); recovered.sendall(frame(410)); ack(recovered, 410)
        check(True, "new device connection succeeds after receiver restart")
        app.close(); app = None; app_thread.join(timeout=5)
        check(not app_thread.is_alive(), "relay stops cleanly and releases listener")

        packaged = HERE / "dist/FireRelay/forwarder/FireRelay.exe"
        if not packaged.exists():
            raise RuntimeError("Build the EXE before verification: python tools/fire-relay/build.py --install-local-receiver")
        shutil.copy2(packaged, forwarder / "FireRelay.exe")
        config_data["listen"]["port"] = port()
        config_path.write_text(json.dumps(config_data), encoding="utf-8")
        exe_environment = os.environ.copy()
        exe_environment["PATH"] = os.pathsep.join([str(Path(os.environ["SystemRoot"]) / "System32"),
                                                   os.environ["SystemRoot"]])
        checked = run([forwarder / "FireRelay.exe", "--check-config"], cwd=WORK, env=exe_environment, timeout=15)
        check(checked.returncode == 0 and b"CONFIG_OK" in checked.stdout,
              "standalone EXE finds config beside itself from a different working directory")
        probe = run([forwarder / "FireRelay.exe", "--probe"], cwd=WORK, env=exe_environment, timeout=15)
        check(probe.returncode == 0, "standalone EXE completes TLS and node authentication")
        exe_log = (WORK / "exe.log").open("wb"); host_files.append(exe_log)
        exe_process = subprocess.Popen([str(forwarder / "FireRelay.exe"), "--headless"], cwd=WORK,
                                       stdout=exe_log, stderr=exe_log, creationflags=FLAGS, env=exe_environment)
        device = None
        for _ in range(100):
            try:
                device = socket.create_connection(("127.0.0.1", config_data["listen"]["port"]), timeout=.1); break
            except OSError:
                if exe_process.poll() is not None:
                    raise RuntimeError("EXE exited: " + str(WORK / "exe.log"))
                time.sleep(.1)
        check(device is not None, "standalone EXE starts device listener without Python/JDK command")
        with device:
            device.settimeout(8); device.sendall(frame(411, True)); ack(device, 411)
        check(sql("USE dachang;SELECT COUNT(*) FROM fire_event WHERE gateway_id=(SELECT gateway_id FROM fire_gateway WHERE allowed_ip='127.0.0.1');") == "1",
              "standalone EXE reports through real Java ingestion into isolated database")
        check(True, "EXE probes and forwards with Python and Java removed from PATH")
        check(not list((HERE / "dist/FireRelay/forwarder").glob("*.p12")), "portable relay excludes receiver private key")
        report = {"result": "FIRE_RELAY_INTEGRATION_OK", "checks": CHECKS,
                  "isolated_mysql": True, "loopback_only": True, "business_database_accessed": False,
                  "actual_devices_contacted": False, "standalone_exe_tested": True, "work_directory": str(WORK)}
        (HERE / "dist/verification.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("FIRE_RELAY_INTEGRATION_OK checks=" + str(len(CHECKS)))
    finally:
        if app:
            app.close()
        if exe_process and exe_process.poll() is None:
            exe_process.terminate(); exe_process.wait(timeout=5)
        if host and host.poll() is None:
            host.terminate(); host.wait(timeout=5)
        run([MYSQL / "mysqladmin.exe", "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             "--port=" + str(dbport), "--user=root", "shutdown"], timeout=10)
        try:
            database.wait(timeout=10)
        except subprocess.TimeoutExpired:
            database.terminate(); database.wait(timeout=5)
        for output in host_files:
            output.close()
        db_log.close()
        print("Verification logs: " + str(WORK))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
