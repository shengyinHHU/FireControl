"""Verify Linux execution, packages and Go/Java interoperability using only disposable fixtures."""
from __future__ import annotations

import hashlib
import argparse
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
import tarfile
import threading
import time

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE / ".build/test-deps"))


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


# Share the existing GB26875 fixture and isolated database setup, without changing
# the Windows program, its deployment package or the configured receiver identity.
fixture = module("windows_relay_verification", ROOT / "tools/fire-relay/verify.py")
build = module("linux_relay_build", HERE / "build.py")
run, check, port = fixture.run, fixture.check, fixture.port
WORK = fixture.WORK
MYSQL = fixture.MYSQL


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--switchable", action="store_true")
    parser.add_argument("--intercom", action="store_true")
    args = parser.parse_args()
    profiled = args.switchable or args.intercom
    suffix = "-intercom" if args.intercom else "-switchable" if args.switchable else ""
    preserved = [ROOT / "tools/fire-relay/dist/FireRelay/FireRelay-Windows-x64.zip",
                 ROOT / "tools/fire-relay/dist/FireRelay/forwarder/FireRelay.exe"]
    original_hashes = {str(p): digest(p) for p in preserved}
    if profiled:
        from cryptography.hazmat.primitives import serialization
        from cryptography.hazmat.primitives.serialization import pkcs12
        release = HERE / ("dist/FireRelay-Kylin-aarch64" + suffix)
        server = HERE / ("dist/FireRelay-Server-Intercom" if args.intercom else "dist/FireRelay-Server-Receiver")
        selected = json.loads((release / "config.json").read_text("utf-8"))["destinations"]["production"]
        _, certificate, _ = pkcs12.load_key_and_certificates((server / "receiver-private/receiver.p12").read_bytes(),
            (server / "receiver-private/keystore-password.secret").read_bytes().strip())
        check(hashlib.sha256(certificate.public_bytes(serialization.Encoding.DER)).hexdigest() == selected["target"]["certificate_sha256"]
              and (server / "receiver-private/relay.secret").read_bytes() == (release / "production-relay.secret").read_bytes(),
              "shipped production certificate and node key match the server deployment pair")
        key = serialization.load_ssh_private_key((release / "ssh/production_ed25519").read_bytes(), password=None)
        public = key.public_key().public_bytes(serialization.Encoding.OpenSSH, serialization.PublicFormat.OpenSSH)
        check((server / "relay-forwarding.pub").read_bytes().startswith(public + b" "),
              "shipped dedicated SSH key matches the restricted server authorization public key")
        old_manifest = json.loads((HERE / "dist/build-manifest.json").read_text("utf-8"))
        check(digest(HERE / "dist/FireRelay-Kylin-aarch64/fire-relay") == old_manifest["elf"]["sha256"],
              "original working ARM64 executable remains unchanged")
        if args.intercom:
            manifest = json.loads((HERE / "dist/build-intercom.json").read_text("utf-8"))
            check(all(digest(Path(name)) == expected for name, expected in manifest["preserved_releases"].items()),
                  "all earlier Linux, server and Windows release archives remain unchanged")
            key = serialization.load_ssh_private_key((release / "ssh/intercom_ed25519").read_bytes(), password=None)
            public = key.public_key().public_bytes(serialization.Encoding.OpenSSH, serialization.PublicFormat.OpenSSH)
            check((server / "intercom-forwarding.pub").read_bytes().startswith(public + b" ")
                  and b'permitlisten="127.0.0.1:33777"' in (server / "intercom-authorized-key.txt").read_bytes(),
                  "dedicated intercom key matches the loopback-restricted reverse forwarding authorization")
            check(selected["intercom"]["cameras"][0]["device_address"] == "10.5.90.150"
                  and selected["intercom"]["cameras"][0]["server_port"] == 33777,
                  "intercom package targets the previously verified camera")
            for folder in (release, server):
                for line in (folder / "SHA256SUMS").read_text("ascii").splitlines():
                    expected, name = line.split("  ", 1)
                    if digest(folder / name) != expected:
                        raise RuntimeError("Package checksum failed: " + str(folder / name))
            check(True, "every shipped forwarder and server file matches its SHA256 checksum")
    package = HERE / ("dist/FireRelay-Kylin-V10-aarch64" + suffix + ".tar.gz")
    elf = build.inspect_elf(HERE / ("dist/FireRelay-Kylin-aarch64" + suffix + "/fire-relay"), 183)
    check(elf["static"], "ARM64 ELF has no dynamic interpreter or shared C runtime")
    with tarfile.open(package) as archive:
        files = {Path(item.name).name: item for item in archive.getmembers() if item.isfile()}
        check(not any(name.endswith(".p12") or "password" in name for name in files),
              "Linux deployment archive excludes receiver private key and password")
        check(files["fire-relay"].mode == 0o755 and files["relay.secret"].mode == 0o600,
              "tar archive retains executable and secret permissions")
        check(archive.extractfile(files["relay.secret"]).read() ==
              (ROOT / "tools/fire-relay/dist/FireRelay/forwarder/relay.secret").read_bytes(),
              "Linux deployment reuses existing node pairing")
    wsl_root = "/mnt/host/d/javaprogram/FireControl/tools/fire-relay-linux"
    linux = run(["wsl.exe", "-d", "docker-desktop", "--", "sh", "-c",
                 f"FIRE_RELAY_TEST_BIN={wsl_root}/.build/fire-relay-linux-amd64 "
                 f"{wsl_root}/.build/relay-linux-tests -test.v -test.timeout 90s"], timeout=110)
    (WORK / "linux-tests.log").write_bytes(linux.stdout)
    print(linux.stdout.decode("utf-8", errors="replace"), flush=True)
    check(linux.returncode == 0 and b"PASS" in linux.stdout,
          "native Linux tests pass including standalone execution with empty runtime PATH")
    syntax = run(["wsl.exe", "-d", "docker-desktop", "--", "sh", "-c",
                  f"for name in run.sh launch.sh create-desktop.sh install-service.sh switch-target.sh server/install-receiver.sh server/authorize-ssh.sh server/install-intercom.sh server/authorize-intercom.sh; do "
                  f"sh -n {wsl_root}/\"$name\" || exit 1; done"], timeout=15)
    check(syntax.returncode == 0, "all deployment scripts pass Linux POSIX shell syntax checks")

    dbport = port()
    init = run([MYSQL / "mysqld.exe", "--no-defaults", "--initialize-insecure",
                "--datadir=" + str(WORK / "mysql")], timeout=60)
    if init.returncode:
        raise RuntimeError(init.stdout.decode(errors="replace"))
    db_log = (WORK / "mysql.log").open("wb")
    database = subprocess.Popen([str(MYSQL / "mysqld.exe"), "--no-defaults", "--datadir=" + str(WORK / "mysql"),
                                 "--port=" + str(dbport), "--bind-address=127.0.0.1", "--mysqlx=OFF",
                                 "--skip-log-bin", "--console"], stdout=db_log, stderr=db_log,
                                creationflags=fixture.FLAGS)
    mysql = [MYSQL / "mysql.exe", "--no-defaults", "--protocol=TCP", "--host=127.0.0.1", "--port=" + str(dbport),
             "--user=root", "--default-character-set=utf8mb4", "--batch", "--skip-column-names"]

    def sql(text):
        result = run(mysql, input=text.encode("utf-8"), timeout=15)
        if result.returncode:
            raise RuntimeError(result.stdout.decode(errors="replace"))
        return result.stdout.decode("utf-8").strip()

    host = app = None
    outputs = []
    try:
        for _ in range(100):
            try:
                sql("SELECT 1")
                break
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
        compiled = run([maven, "-o", "-X", "-pl", "ruoyi-admin", "-am", "-DskipTests", "compile"],
                       cwd=ROOT / "RuoYi-Vue-master", timeout=180)
        (WORK / "maven.log").write_bytes(compiled.stdout)
        if compiled.returncode:
            raise RuntimeError("Backend compilation failed: " + str(WORK / "maven.log"))
        paths = re.findall(r"compilePath = \[([^\r\n]+)\]", compiled.stdout.decode(errors="replace"))
        cp = os.pathsep.join(paths[-1].split(", "))
        classes = WORK / "classes"
        classes.mkdir()
        compiled = run(["javac", "-encoding", "UTF-8", "-cp", cp, "-d", classes,
                        ROOT / "tools/fire-relay/tests/FireRelayTestHost.java"], timeout=30)
        if compiled.returncode:
            raise RuntimeError(compiled.stdout.decode(errors="replace"))
        forwarder, private = fixture.provision(WORK / "paired", target="127.0.0.1", bind="127.0.0.1")
        config_path = forwarder / "config.json"
        config = json.loads(config_path.read_text("utf-8"))
        config["listen"] = {"address": "127.0.0.1", "port": port()}
        config["target"]["port"] = receiver_port = port()
        config["reconnect_seconds"] = 1
        config["connect_timeout_seconds"] = 2
        config_path.write_text(json.dumps(config), encoding="utf-8")
        direct_port = port()

        def start_host():
            output = (WORK / ("host-" + str(len(outputs)) + ".log")).open("wb")
            outputs.append(output)
            process = subprocess.Popen(["java", "-Dfile.encoding=UTF-8", "-cp", str(classes) + os.pathsep + cp,
                                        "FireRelayTestHost", str(dbport), str(ROOT), str(private), "unused", "unused",
                                        str(receiver_port), str(direct_port)], stdin=subprocess.PIPE,
                                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT, creationflags=fixture.FLAGS)
            ready = threading.Event()

            def read():
                for line in process.stdout:
                    output.write(line)
                    output.flush()
                    if line.strip() == b"READY":
                        ready.set()

            threading.Thread(target=read, daemon=True).start()
            if not ready.wait(20):
                process.terminate()
                process.wait(timeout=5)
                raise RuntimeError("Java test host not ready: " + str(WORK))
            return process

        def stop_host(process):
            process.stdin.write(b"STOP\n")
            process.stdin.flush()
            process.wait(timeout=10)

        def connect_device():
            return socket.create_connection(("127.0.0.1", config["listen"]["port"]), timeout=3)

        host = start_host()
        executable = forwarder / "fire-relay.exe"
        shutil.copy2(HERE / ".build/interop-relay.exe", executable)
        environment = os.environ.copy()
        environment["PATH"] = os.pathsep.join([str(Path(os.environ["SystemRoot"]) / "System32"), os.environ["SystemRoot"]])
        probe = run([executable, "--probe"], cwd=WORK, env=environment, timeout=10)
        check(probe.returncode == 0 and b"PROBE_OK" in probe.stdout,
              "Go relay authenticates against actual Java TLS receiver without runtime tools")
        check(sql("USE dachang;SELECT COUNT(*) FROM fire_protocol_message;") == "0",
              "Go authenticated health probe creates no business messages")
        for field, bad_value in [("certificate_sha256", "0" * 64), ("secret_file", "bad.secret")]:
            original = config["target"][field]
            (forwarder / "bad.secret").write_text("x" * 64, encoding="ascii")
            config["target"][field] = bad_value
            config_path.write_text(json.dumps(config), encoding="utf-8")
            rejected = run([executable, "--probe"], cwd=WORK, env=environment, timeout=10)
            check(rejected.returncode != 0, "Java interop rejects invalid " + field)
            config["target"][field] = original
        config_path.write_text(json.dumps(config), encoding="utf-8")
        log = (WORK / "go-relay.log").open("wb")
        outputs.append(log)
        app = subprocess.Popen([str(executable), "--headless"], cwd=WORK, env=environment,
                               stdout=log, stderr=log, creationflags=fixture.FLAGS)
        device = None
        for _ in range(100):
            try:
                device = connect_device()
                break
            except OSError:
                if app.poll() is not None:
                    raise RuntimeError("Go relay exited unexpectedly")
                time.sleep(.05)
        check(device is not None, "Go executable starts configured device listener")
        with device:
            device.settimeout(8)
            device.sendall(b"\0" * 180000 + fixture.frame(701, True)[:7])
            device.sendall(fixture.frame(701, True)[7:] + fixture.frame(702))
            fixture.ack(device, 701)
            fixture.ack(device, 702)
        check(sql("USE dachang;SELECT COUNT(*) FROM fire_event WHERE gateway_id=(SELECT gateway_id FROM fire_gateway WHERE allowed_ip='127.0.0.1');") == "1",
              "Go relay preserves original source IP and returns ACK after actual event commit")
        stop_host(host)
        host = None
        with connect_device() as disconnected:
            disconnected.settimeout(5)
            disconnected.sendall(fixture.frame(703))
            try:
                value = disconnected.recv(30)
            except OSError:
                value = b""
            check(not value, "receiver outage yields no fabricated ACK through Go relay")
        host = start_host()
        with connect_device() as recovered:
            recovered.settimeout(8)
            recovered.sendall(fixture.frame(704))
            fixture.ack(recovered, 704)
        check(True, "new device connection recovers after actual Java receiver restart")
        sql("USE dachang;RENAME TABLE fire_protocol_message TO fire_protocol_message_saved;")
        try:
            with connect_device() as broken:
                broken.settimeout(5)
                broken.sendall(fixture.frame(705))
                try:
                    value = broken.recv(30)
                except OSError:
                    value = b""
                check(not value, "database persistence failure yields no successful ACK through Go relay")
        finally:
            sql("USE dachang;RENAME TABLE fire_protocol_message_saved TO fire_protocol_message;")
        if profiled:
            app.terminate()
            app.wait(timeout=5)
            app = None
            from ssh_fixture import verify_switching
            verify_switching(fixture, executable, forwarder, config, environment, receiver_port, WORK, sql)
            (HERE / "dist/verification-ssh.json").write_text(json.dumps({
                "result": "LOCAL_OPENSSH_JAVA_VERIFIED", "real_server_contacted": False,
                "ssh_library_fixture": "paramiko (test only)", "ssh_client": "Windows OpenSSH",
                "go_source": "same source as ARM64 release", "work_directory": str(WORK)
            }, indent=2) + "\n", encoding="utf-8")
        check({str(p): digest(p) for p in preserved} == original_hashes,
              "existing Windows EXE and deployment ZIP remain byte-for-byte unchanged")
        report = {"result": "LINUX_RELAY_VERIFIED", "checks": fixture.CHECKS,
                  "isolated_mysql": True, "loopback_only": True, "business_database_accessed": False,
                  "native_linux_test_arch": "amd64", "arm64_elf": elf, "arm64_executed": False,
                  "actual_kylin_tested": False, "java_interop": "same Go source compiled for Windows",
                  "windows_package_preserved": True, "work_directory": str(WORK)}
        (HERE / ("dist/verification" + suffix + ".json")).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("LINUX_RELAY_VERIFIED checks=" + str(len(fixture.CHECKS)), flush=True)
    finally:
        for process in [app, host]:
            if process and process.poll() is None:
                process.terminate()
                process.wait(timeout=5)
        run([MYSQL / "mysqladmin.exe", "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             "--port=" + str(dbport), "--user=root", "shutdown"], timeout=10)
        try:
            database.wait(timeout=10)
        except subprocess.TimeoutExpired:
            database.terminate()
            database.wait(timeout=5)
        for output in outputs:
            output.close()
        db_log.close()
        print("Verification logs: " + str(WORK), flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
