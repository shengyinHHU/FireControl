"""Loopback-only real OpenSSH reverse forwarding tests; no camera/server contact."""
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE / ".build/test-deps"))
from ssh_fixture import LocalSSH


def free_port():
    with socket.socket() as stream:
        stream.bind(("127.0.0.1", 0))
        return stream.getsockname()[1]


def main():
    work = Path(tempfile.mkdtemp(prefix="fire-intercom-verify-"))
    port = free_port()
    fixture = LocalSSH(work / "ssh", free_port(), reverse_port=port)
    try:
        configuration = {"ssh": {"host": "127.0.0.1", "port": fixture.port, "username": "fixture",
            "private_key_file": str(fixture.key_path), "known_hosts_file": str(fixture.known_hosts)},
            "server_port": port, "control_port": fixture.control_port}
        path = work / "fixture.json"
        path.write_text(json.dumps(configuration), encoding="utf-8")
        environment = dict(os.environ, FIRE_RELAY_SSH_FIXTURE=str(path))
        environment["PATH"] = str(Path(os.environ["SystemRoot"]) / "System32/OpenSSH") + os.pathsep + environment["PATH"]
        result = subprocess.run([str(HERE / ".build/relay-windows-tests.exe"), "-test.v", "-test.run", "TestSSHIntercom", "-test.timeout", "60s"],
            env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=75,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        (work / "tests.log").write_bytes(result.stdout)
        print(result.stdout.decode("utf-8", errors="replace"))
        if result.returncode:
            raise RuntimeError("SSH intercom verification failed: " + str(work))
        report = {"result": "INTERCOM_SSH_VERIFIED", "loopback_only": True, "real_openssh_client": True,
            "bidirectional_binary_data": True, "parallel_connections": 4, "reconnect_after_ssh_drop": True,
            "fire_reports_survive_intercom_rejection": True, "actual_camera_audio_tested": False,
            "actual_kylin_tested": False, "public_server_contacted": False, "work_directory": str(work)}
        (HERE / "dist/verification-intercom-ssh.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print("INTERCOM_SSH_VERIFIED")
    finally:
        fixture.close()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
