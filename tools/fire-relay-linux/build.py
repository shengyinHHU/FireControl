"""Cross-compile a static Kylin ARM64 relay; preserve the existing Windows release and pairing."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tarfile
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
BUILD = HERE / ".build"


def toolchain():
    installed = shutil.which("go")
    local = BUILD / "toolchain/go/bin" / ("go.exe" if os.name == "nt" else "go")
    return installed or (str(local) if local.exists() else None)


def compile_go(go, operating_system, arch, destination, *extra):
    env = os.environ.copy()
    env.update({"CGO_ENABLED": "0", "GOOS": operating_system, "GOARCH": arch,
                "GOARM64": "v8.0", "GOAMD64": "v1", "GOTOOLCHAIN": "local",
                "GOCACHE": str(BUILD / "cache")})
    destination.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([go, "build", "-trimpath", "-ldflags=-s -w", "-o", str(destination), *extra, "."],
                   cwd=HERE, env=env, check=True)


def inspect_elf(path, machine):
    value = path.read_bytes()
    if value[:6] != b"\x7fELF\x02\x01" or struct.unpack_from("<H", value, 18)[0] != machine:
        raise RuntimeError("Unexpected ELF platform: " + str(path))
    offset = struct.unpack_from("<Q", value, 32)[0]
    entry_size, count = struct.unpack_from("<HH", value, 54)
    kinds = [struct.unpack_from("<I", value, offset + i * entry_size)[0] for i in range(count)]
    if 2 in kinds or 3 in kinds:
        raise RuntimeError("ELF unexpectedly requires a dynamic linker/shared C runtime")
    return {"format": "ELF64 little-endian", "machine": machine, "static": True,
            "sha256": hashlib.sha256(value).hexdigest(), "bytes": len(value)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pair", type=Path, default=ROOT / "tools/fire-relay/dist/FireRelay/forwarder")
    parser.add_argument("--tests", action="store_true", help="Also compile Linux x64 tests and a Windows interop fixture")
    args = parser.parse_args()
    go = toolchain()
    if not go:
        raise SystemExit("Go toolchain is needed on the build computer only; install Go 1.23+ or provide it on PATH.")
    source_config = args.pair / "config.json"
    source_secret = args.pair / "relay.secret"
    if not source_config.is_file() or not source_secret.is_file():
        raise SystemExit("Existing paired config.json / relay.secret not found; use --pair to select them.")
    # Read configuration but never print the key or regenerate the receiver identity.
    config = json.loads(source_config.read_text(encoding="utf-8-sig"))
    secret = source_secret.read_bytes()
    output = HERE / "dist/FireRelay-Kylin-aarch64"
    output.mkdir(parents=True, exist_ok=True)
    for name, data in (("config.json", (json.dumps(config, ensure_ascii=False, indent=2) + "\n").encode()),
                       ("relay.secret", secret)):
        destination = output / name
        if destination.exists() and destination.read_bytes() != data:
            raise SystemExit("Refusing to overwrite changed deployment configuration: " + str(destination))
        destination.write_bytes(data)
    compile_go(go, "linux", "arm64", output / "fire-relay")
    arm = inspect_elf(output / "fire-relay", 183)
    for name in ("run.sh", "launch.sh", "create-desktop.sh", "install-service.sh", "firecontrol-relay.service", "README.md"):
        text = (HERE / name).read_text(encoding="utf-8").replace("\r\n", "\n")
        (output / name).write_bytes(text.encode("utf-8"))
    checksums = "".join(hashlib.sha256(item.read_bytes()).hexdigest() + "  " + item.name + "\n"
                        for item in sorted(output.iterdir()) if item.is_file() and item.name != "SHA256SUMS")
    (output / "SHA256SUMS").write_bytes(checksums.encode("ascii"))
    def mode(name):
        return 0o755 if name == "fire-relay" or name.endswith(".sh") else (0o600 if name in ("config.json", "relay.secret") else 0o644)
    tar_path = HERE / "dist/FireRelay-Kylin-V10-aarch64.tar.gz"
    with tarfile.open(tar_path, "w:gz") as archive:
        root = tarfile.TarInfo(output.name); root.type = tarfile.DIRTYPE; root.mode = 0o755
        archive.addfile(root)
        for item in sorted(output.iterdir()):
            if item.is_file():
                info = archive.gettarinfo(str(item), output.name + "/" + item.name)
                info.mode = mode(item.name); info.uid = info.gid = 0; info.uname = info.gname = ""
                with item.open("rb") as stream:
                    archive.addfile(info, stream)
    with zipfile.ZipFile(HERE / "dist/FireRelay-Kylin-V10-aarch64.zip", "w", zipfile.ZIP_DEFLATED) as archive:
        for item in sorted(output.iterdir()):
            if item.is_file():
                info = zipfile.ZipInfo(output.name + "/" + item.name); info.create_system = 3
                info.external_attr = (0o100000 | mode(item.name)) << 16
                archive.writestr(info, item.read_bytes(), compress_type=zipfile.ZIP_DEFLATED)
    manifest = {"architecture": "linux/arm64 (ARMv8.0)", "target_os": "Kylin Linux Desktop V10 SP1",
                "elf": arm, "go": subprocess.check_output([go, "version"], text=True).strip(),
                "pair_reused": True, "receiver_private_key_included": False,
                "archive": str(tar_path)}
    if args.tests:
        compile_go(go, "linux", "amd64", BUILD / "fire-relay-linux-amd64")
        inspect_elf(BUILD / "fire-relay-linux-amd64", 62)
        compile_go(go, "windows", "amd64", BUILD / "interop-relay.exe")
        env = os.environ.copy()
        env.update({"CGO_ENABLED": "0", "GOOS": "linux", "GOARCH": "amd64", "GOTOOLCHAIN": "local", "GOCACHE": str(BUILD / "cache")})
        subprocess.run([go, "test", "-c", "-o", str(BUILD / "relay-linux-tests")], cwd=HERE, env=env, check=True)
    (HERE / "dist/build-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("ARM64_PACKAGE=" + str(tar_path))
    print("ELF_ARM64_STATIC_OK")


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    main()
