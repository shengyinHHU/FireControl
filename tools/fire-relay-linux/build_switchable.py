"""Create separate switchable ARM64 and server deployment releases; never touch v1 releases."""
import argparse
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import zipfile

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

import build

HERE, ROOT, BUILD = build.HERE, build.ROOT, build.BUILD


def archive(directory, destination):
    with tarfile.open(destination, "w:gz") as package:
        for source in [directory, *sorted(directory.rglob("*"))]:
            info = package.gettarinfo(str(source), str(Path(directory.name) / source.relative_to(directory)).replace("\\", "/"))
            info.uid = info.gid = 0
            info.uname = info.gname = ""
            if source.is_dir():
                info.mode = 0o750
                package.addfile(info)
            else:
                info.mode = (0o755 if source.name == "fire-relay" or source.suffix == ".sh" else
                             0o600 if source.suffix in (".secret", ".p12") or source.name in ("config.json", "production_ed25519", "intercom_ed25519") else 0o644)
                with source.open("rb") as stream:
                    package.addfile(info, stream)


def copy_text(source, destination):
    destination.write_bytes(source.read_text(encoding="utf-8").replace("\r\n", "\n").encode("utf-8"))


def checksums(directory):
    text = "".join(hashlib.sha256(p.read_bytes()).hexdigest() + "  " + p.relative_to(directory).as_posix() + "\n"
                   for p in sorted(directory.rglob("*")) if p.is_file() and p.name != "SHA256SUMS")
    (directory / "SHA256SUMS").write_bytes(text.encode("ascii"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tests", action="store_true")
    args = parser.parse_args()
    go = build.toolchain()
    if not go:
        raise SystemExit("Go toolchain not found on build computer")
    paired = ROOT / "tools/fire-relay/dist/FireRelay/forwarder"
    production = BUILD / "production-pair"
    if not production.exists():
        spec = importlib.util.spec_from_file_location("windows_pair_builder", ROOT / "tools/fire-relay/build.py")
        provisioner = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(provisioner)
        provisioner.provision(production, target="218.94.2.84", bind="127.0.0.1")
    private = production / "receiver-private"
    credentials = BUILD / "production-ssh"
    credentials.mkdir(parents=True, exist_ok=True)
    key = credentials / "production_ed25519"
    public = credentials / "relay-forwarding.pub"
    if not key.exists():
        generated = Ed25519PrivateKey.generate()
        key.write_bytes(generated.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.OpenSSH,
                                                serialization.NoEncryption()))
        public.write_bytes(generated.public_key().public_bytes(serialization.Encoding.OpenSSH,
                                                               serialization.PublicFormat.OpenSSH) + b" firecontrol-relay-production\n")
    if not public.exists():
        raise SystemExit("Existing SSH pair incomplete; private key was not replaced")
    known_hosts = Path(os.environ["USERPROFILE"]) / ".ssh/firecontrol_panel_known_hosts"
    if not known_hosts.is_file():
        raise SystemExit("Previously trusted server known_hosts is missing; do not invent a host fingerprint")
    output = HERE / "dist/FireRelay-Kylin-aarch64-switchable"
    output.mkdir(parents=True, exist_ok=True)
    ssh_directory = output / "ssh"
    ssh_directory.mkdir(exist_ok=True)
    for source, destination in [(paired / "relay.secret", output / "relay.secret"),
                                (production / "forwarder/relay.secret", output / "production-relay.secret"),
                                (key, ssh_directory / "production_ed25519"),
                                (known_hosts, ssh_directory / "production_known_hosts")]:
        if destination.exists() and destination.read_bytes() != source.read_bytes():
            raise SystemExit("Refusing to change existing release credentials: " + str(destination))
        shutil.copy2(source, destination)
    dev = json.loads((paired / "config.json").read_text("utf-8-sig"))
    prod = json.loads((production / "forwarder/config.json").read_text("utf-8"))
    config = {k: v for k, v in dev.items() if k not in ("node_id", "target", "ssh")}
    config["active_target"] = "development"
    ssh = prod["ssh"]
    ssh.update({"enabled": True, "username": "root", "private_key_file": "ssh/production_ed25519",
                "known_hosts_file": "ssh/production_known_hosts"})
    prod["target"]["secret_file"] = "production-relay.secret"
    config["destinations"] = {
        "development": {"node_id": dev["node_id"], "target": dev["target"], "ssh": dev["ssh"]},
        "production": {"node_id": prod["node_id"], "target": prod["target"], "ssh": ssh}}
    config_path = output / "config.json"
    content = (json.dumps(config, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    if config_path.exists() and config_path.read_bytes() != content:
        raise SystemExit("Deployment config was edited; it has not been overwritten")
    config_path.write_bytes(content)
    build.compile_go(go, "linux", "arm64", output / "fire-relay")
    elf = build.inspect_elf(output / "fire-relay", 183)
    for name in ("run.sh", "launch.sh", "create-desktop.sh", "switch-target.sh", "install-service.sh", "firecontrol-relay.service"):
        copy_text(HERE / name, output / name)
    copy_text(HERE / "README-switchable.md", output / "README.md")
    checksums(output)
    arm_archive = HERE / "dist/FireRelay-Kylin-V10-aarch64-switchable.tar.gz"
    archive(output, arm_archive)
    server = HERE / "dist/FireRelay-Server-Receiver"
    server.mkdir(exist_ok=True)
    receiver = server / "receiver-private"
    receiver.mkdir(exist_ok=True)
    for name in ("receiver.p12", "keystore-password.secret", "relay.secret"):
        shutil.copy2(private / name, receiver / name)
    for name in ("install-receiver.sh", "authorize-ssh.sh", "README.md"):
        copy_text(HERE / "server" / name, server / name)
    shutil.copy2(public, server / "relay-forwarding.pub")
    jar = ROOT / "RuoYi-Vue-master/ruoyi-admin/target/ruoyi-admin.jar"
    with zipfile.ZipFile(jar) as package:
        system = next(n for n in package.namelist() if n.startswith("BOOT-INF/lib/ruoyi-system-"))
        with zipfile.ZipFile(io.BytesIO(package.read(system))) as library:
            if "com/ruoyi/system/fire/FireRelayServer.class" not in library.namelist():
                raise SystemExit("Backend JAR lacks the relay receiver")
    shutil.copy2(jar, server / "ruoyi-admin.jar")
    checksums(server)
    server_archive = HERE / "dist/FireRelay-Server-Receiver.tar.gz"
    archive(server, server_archive)
    if args.tests:
        build.compile_go(go, "linux", "amd64", BUILD / "fire-relay-linux-amd64")
        build.compile_go(go, "windows", "amd64", BUILD / "interop-relay.exe")
        env = os.environ.copy()
        env.update({"CGO_ENABLED": "0", "GOOS": "linux", "GOARCH": "amd64", "GOTOOLCHAIN": "local", "GOCACHE": str(BUILD / "cache")})
        subprocess.run([go, "test", "-c", "-o", str(BUILD / "relay-linux-tests")], cwd=HERE, env=env, check=True)
    manifest = {"release": "1.1.0-switchable", "arm64": elf, "server_jar_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
                "server_contacted": False, "default_target": "development", "server_private_key_in_forwarder": False}
    (HERE / "dist/build-switchable.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("KYLIN_PACKAGE=" + str(arm_archive))
    print("SERVER_PACKAGE=" + str(server_archive))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
