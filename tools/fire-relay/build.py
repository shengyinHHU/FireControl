"""Build the standalone Windows EXE and provision a private, paired receiver configuration."""
import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import zipfile

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]


def provision(directory, node="site-01", target="10.214.18.130", bind="0.0.0.0", port=22000):
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=False)
    private = directory / "receiver-private"
    forwarder = directory / "forwarder"
    private.mkdir(); forwarder.mkdir()
    key = rsa.generate_private_key(public_exponent=65537, key_size=3072)
    subject = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "FireControl Relay " + node)])
    now = datetime.now(timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(key.public_key())
            .serial_number(x509.random_serial_number()).not_valid_before(now - timedelta(minutes=5))
            .not_valid_after(now + timedelta(days=730)).sign(key, hashes.SHA256()))
    password = secrets.token_urlsafe(32)
    secret = secrets.token_hex(32)
    p12 = pkcs12.serialize_key_and_certificates(b"fire-relay", key, cert, None,
                                               serialization.BestAvailableEncryption(password.encode()))
    (private / "receiver.p12").write_bytes(p12)
    (private / "keystore-password.secret").write_text(password, encoding="utf-8")
    (private / "relay.secret").write_text(secret, encoding="utf-8")
    (forwarder / "relay.secret").write_text(secret, encoding="utf-8")
    config = json.loads((HERE / "config.example.json").read_text(encoding="utf-8"))
    config["node_id"] = node
    config["target"]["address"] = target
    config["target"]["port"] = port
    config["target"]["certificate_sha256"] = hashlib.sha256(cert.public_bytes(serialization.Encoding.DER)).hexdigest()
    config["ssh"]["remote_port"] = port
    (forwarder / "config.json").write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    properties = ["# Local receiver credentials. Never commit or copy this directory to the relay PC.",
                  "fire.relay.enabled=true", "fire.relay.bind-address=" + bind, "fire.relay.port=" + str(port),
                  "fire.relay.node-id=" + node, "fire.relay.allowed-source-cidrs=10.5.89.0/24,10.5.90.0/24",
                  "fire.relay.key-store=" + (private / "receiver.p12").resolve().as_posix(),
                  "fire.relay.key-store-password-file=" + (private / "keystore-password.secret").resolve().as_posix(),
                  "fire.relay.secret-file=" + (private / "relay.secret").resolve().as_posix()]
    (private / "fire-relay-local.properties").write_text("\n".join(properties) + "\n", encoding="utf-8")
    return forwarder, private


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=HERE / "dist" / "FireRelay")
    parser.add_argument("--skip-exe", action="store_true")
    parser.add_argument("--reuse-pair", action="store_true", help="复用已有配对密钥，仅更新EXE、说明和ZIP")
    parser.add_argument("--install-local-receiver", action="store_true")
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists() and not args.reuse_pair:
        raise SystemExit("输出目录已存在；请选择新的 --output。现有证书和密钥不会被覆盖。")
    if args.reuse_pair:
        forwarder, private = output / "forwarder", output / "receiver-private"
        for required in (forwarder / "config.json", forwarder / "relay.secret", private / "receiver.p12",
                         private / "keystore-password.secret", private / "fire-relay-local.properties"):
            if not required.is_file():
                raise SystemExit("已有配对文件缺失：" + str(required))
    if not args.skip_exe:
        subprocess.run([sys.executable, "-m", "PyInstaller", "--noconfirm", "--clean", "--onefile",
                        "--name", "FireRelay", "--distpath", str(HERE / ".build" / "bin"),
                        "--workpath", str(HERE / ".build" / "work"), "--specpath", str(HERE / ".build"),
                        str(HERE / "relay.py")], check=True, cwd=HERE)
    if not args.reuse_pair:
        forwarder, private = provision(output)
    if not args.skip_exe:
        shutil.copy2(HERE / ".build" / "bin" / "FireRelay.exe", forwarder / "FireRelay.exe")
    # Windows PowerShell 5.1 needs a BOM to reliably read Chinese UTF-8 source.
    (forwarder / "install-startup.ps1").write_text(
        (HERE / "install-startup.ps1").read_text(encoding="utf-8"), encoding="utf-8-sig")
    shutil.copy2(HERE / "README.md", forwarder / "使用说明.md")
    if args.install_local_receiver:
        destination = ROOT / "RuoYi-Vue-master/config/fire-relay-local.properties"
        if destination.exists() and destination.read_bytes() != (private / "fire-relay-local.properties").read_bytes():
            raise SystemExit("本地接收端配置已存在，未覆盖：" + str(destination))
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(private / "fire-relay-local.properties", destination)
    # The receiver private key is deliberately excluded from the portable relay ZIP.
    archive = output / "FireRelay-Windows-x64.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as package:
        for source in forwarder.rglob("*"):
            if source.is_file():
                package.write(source, Path("FireRelay") / source.relative_to(forwarder))
    print("FORWARDER=" + str(forwarder))
    print("RECEIVER_CONFIG=" + str(private / "fire-relay-local.properties"))
    print("ZIP=" + str(archive))


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    main()
