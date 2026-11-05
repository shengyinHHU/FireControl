"""Build the backend and verify portable TCP binding without starting business services."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "RuoYi-Vue-master"
WORK = Path(tempfile.mkdtemp(prefix="fire-tcp-binding-"))


def run(arguments, timeout):
    return subprocess.run(
        [str(argument) for argument in arguments], cwd=BACKEND,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )


maven = shutil.which("mvn.cmd") or shutil.which("mvn")
if not maven:
    raise RuntimeError("Maven is not available on PATH")
build = run([maven, "-o", "-X", "-pl", "ruoyi-admin", "-am", "-DskipTests", "package"], 180)
build_log = build.stdout.decode("utf-8", errors="replace")
(WORK / "maven.log").write_text(build_log, encoding="utf-8")
if build.returncode:
    raise RuntimeError(f"Backend build failed; see {WORK / 'maven.log'}")
print("Backend package: BUILD SUCCESS", flush=True)
paths = re.findall(r"compilePath = \[([^\r\n]+)\]", build_log)
if not paths:
    raise RuntimeError("Maven compiler dependency path not found")
classpath = os.pathsep.join(paths[-1].split(", "))
yaml = BACKEND / "ruoyi-admin/target/classes/application.yml"
jar = BACKEND / "ruoyi-admin/target/ruoyi-admin.jar"
with zipfile.ZipFile(jar) as archive:
    assert archive.read("BOOT-INF/classes/application.yml") == yaml.read_bytes(), "Packaged YAML mismatch"
classes = WORK / "classes"
classes.mkdir()
compiled = run(["javac", "-encoding", "UTF-8", "-cp", classpath, "-d", classes,
                ROOT / "tools/device-capture/java/FireTcpBindingTest.java"], 30)
if compiled.returncode:
    raise RuntimeError(compiled.stdout.decode("utf-8", errors="replace"))
tested = run(["java", "-Dfile.encoding=UTF-8", "-cp", str(classes) + os.pathsep + classpath,
              "FireTcpBindingTest", yaml], 30)
output = tested.stdout.decode("utf-8", errors="replace")
print(output)
if tested.returncode:
    raise RuntimeError("TCP binding verification failed")
report = dict(
    backend_package="BUILD SUCCESS", packaged_configuration_verified=True,
    result=output.splitlines()[-1], business_database_accessed=False,
    external_services_started=False, device_port_2000_used=False,
    cases=["default wildcard bind", "environment address and port override", "TCP disable switch",
           "real socket connections and port release", "port conflict", "unavailable local address"],
)
destination = ROOT / "docs/verification/fire-tcp-binding.json"
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Verified. Build log: {WORK / 'maven.log'}")
