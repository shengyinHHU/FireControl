"""Create separate 1.2 intercom releases from the existing pair, preserving all old archives."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
import build
from build_switchable import archive, checksums, copy_text

HERE, ROOT, BUILD = build.HERE, build.ROOT, build.BUILD


def copy_paired(source, destination):
    if destination.exists() and destination.read_bytes() != source.read_bytes():
        raise RuntimeError("Existing deployment identity/config was not overwritten: " + str(destination))
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tests", action="store_true")
    args = parser.parse_args()
    previous = HERE / "dist/FireRelay-Kylin-aarch64-switchable"
    old_server = HERE / "dist/FireRelay-Server-Receiver"
    preserved = [HERE / "dist/FireRelay-Kylin-V10-aarch64.tar.gz", HERE / "dist/FireRelay-Kylin-V10-aarch64-switchable.tar.gz",
        HERE / "dist/FireRelay-Server-Receiver.tar.gz", previous / "fire-relay",
        ROOT / "tools/fire-relay/dist/FireRelay/FireRelay-Windows-x64.zip"]
    original = {str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in preserved}
    credentials = BUILD / "production-intercom-ssh"
    credentials.mkdir(parents=True, exist_ok=True)
    key = credentials / "intercom_ed25519"
    public = credentials / "intercom-forwarding.pub"
    if not key.exists():
        generated = Ed25519PrivateKey.generate()
        key.write_bytes(generated.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.OpenSSH, serialization.NoEncryption()))
        public.write_bytes(generated.public_key().public_bytes(serialization.Encoding.OpenSSH, serialization.PublicFormat.OpenSSH) + b" firecontrol-intercom-production\n")
    if not public.is_file():
        raise RuntimeError("Incomplete existing intercom SSH key pair")
    output = HERE / "dist/FireRelay-Kylin-aarch64-intercom"
    output.mkdir(parents=True, exist_ok=True)
    for name in ("relay.secret", "production-relay.secret", "ssh/production_ed25519", "ssh/production_known_hosts"):
        copy_paired(previous / name, output / name)
    copy_paired(key, output / "ssh/intercom_ed25519")
    cfg = json.loads((previous / "config.json").read_text("utf-8"))
    cfg["destinations"]["production"]["intercom"] = {"enabled": True, "private_key_file": "ssh/intercom_ed25519",
        "cameras": [{"name": "shanpan", "device_address": "10.5.90.150", "device_port": 37777, "server_port": 33777}]}
    data = (json.dumps(cfg, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    cfg_path = output / "config.json"
    if cfg_path.exists() and cfg_path.read_bytes() != data:
        raise RuntimeError("Existing intercom release configuration was not overwritten")
    cfg_path.write_bytes(data)
    go = build.toolchain()
    build.compile_go(go, "linux", "arm64", output / "fire-relay")
    elf = build.inspect_elf(output / "fire-relay", 183)
    for name in ("run.sh", "launch.sh", "create-desktop.sh", "switch-target.sh", "install-service.sh", "firecontrol-relay.service"):
        copy_text(HERE / name, output / name)
    copy_text(HERE / "README-intercom.md", output / "README.md")
    checksums(output)
    arm_archive = HERE / "dist/FireRelay-Kylin-V10-aarch64-intercom.tar.gz"
    archive(output, arm_archive)
    server = HERE / "dist/FireRelay-Server-Intercom"
    server.mkdir(parents=True, exist_ok=True)
    for name in ("receiver.p12", "keystore-password.secret", "relay.secret"):
        copy_paired(old_server / "receiver-private" / name, server / "receiver-private" / name)
    copy_paired(old_server / "relay-forwarding.pub", server / "relay-forwarding.pub")
    copy_paired(public, server / public.name)
    line = 'restrict,port-forwarding,permitopen="127.0.0.1:22000",permitlisten="127.0.0.1:33777",command="/bin/false" ' + public.read_text("ascii").strip() + "\n"
    (server / "intercom-authorized-key.txt").write_bytes(line.encode("ascii"))
    for name in ("install-receiver.sh", "authorize-ssh.sh", "install-intercom.sh", "authorize-intercom.sh", "fire-intercom.properties"):
        copy_text(HERE / "server" / name, server / name)
    copy_text(HERE / "server/README-intercom.md", server / "README.md")
    copy_text(ROOT / "RuoYi-Vue-master/config/README-fire-relay.md", server / "PROJECT-DEPLOYMENT.md")
    project_config = server / "project-config/config"
    project_config.mkdir(parents=True, exist_ok=True)
    copy_text(ROOT / "RuoYi-Vue-master/config/fire-relay-production.example.properties", project_config / "fire-relay-local.properties")
    for name in ("receiver.p12", "keystore-password.secret", "relay.secret"):
        copy_paired(old_server / "receiver-private" / name, project_config / "fire-relay-private" / name)
    native = ROOT / "RuoYi-Vue-master/vendor/dahua-netsdk/native/linux64"
    for path in sorted(native.glob("*.so*")):
        copy_paired(path, server / "native/linux64" / path.name)
    if not (server / "native/linux64/libdhnetsdk.so").is_file():
        raise RuntimeError("Linux x86-64 SDK missing from deployment package")
    jar = ROOT / "RuoYi-Vue-master/ruoyi-admin/target/ruoyi-admin.jar"
    with zipfile.ZipFile(jar) as package:
        name = next(n for n in package.namelist() if n.startswith("BOOT-INF/lib/ruoyi-system-"))
        with zipfile.ZipFile(io.BytesIO(package.read(name))) as library:
            if b"resolveEndpoint" not in library.read("com/ruoyi/system/fire/dahua/DahuaProperties.class"):
                raise RuntimeError("Build backend JAR before packaging: tunnel resolver missing")
    shutil.copy2(jar, server / "ruoyi-admin.jar")
    checksums(server)
    server_archive = HERE / "dist/FireRelay-Server-Intercom.tar.gz"
    archive(server, server_archive)
    if args.tests:
        build.compile_go(go, "linux", "amd64", BUILD / "fire-relay-linux-amd64")
        build.compile_go(go, "windows", "amd64", BUILD / "interop-relay.exe")
        for platform, destination in (("linux", "relay-linux-tests"), ("windows", "relay-windows-tests.exe")):
            env = dict(os.environ, CGO_ENABLED="0", GOOS=platform, GOARCH="amd64", GOTOOLCHAIN="local", GOCACHE=str(BUILD / "cache"))
            subprocess.run([go, "test", "-c", "-o", str(BUILD / destination)], cwd=HERE, env=env, check=True)
    if {str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in preserved} != original:
        raise RuntimeError("Previous release archive changed")
    manifest = {"release": "1.2.0-intercom", "arm64": elf, "server_jar_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
        "public_server_contacted": False, "actual_camera_audio_tested": False, "default_target": cfg["active_target"],
        "preserved_releases": original, "receiver_pair_reused": True, "intercom_ssh_key_separate": True}
    (HERE / "dist/build-intercom.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("KYLIN_PACKAGE=" + str(arm_archive))
    print("SERVER_PACKAGE=" + str(server_archive))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
