"""Bounded integration verification in an isolated temporary MySQL. Never reads project credentials."""
import os,sys,subprocess,tempfile,time,socket,re,json,zipfile
from pathlib import Path
sys.stdout.reconfigure(encoding="utf-8",errors="replace")
ROOT=Path(__file__).resolve().parents[2]
BIN=Path(r"C:\Program Files\MySQL\MySQL Server 8.0\bin")
work=Path(tempfile.mkdtemp(prefix="fire-device-integration-"))
flags=getattr(subprocess,"CREATE_NO_WINDOW",0)
def run(args,**kw):
 return subprocess.run([str(x) for x in args],creationflags=flags,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kw)
init=run([BIN/"mysqld.exe","--no-defaults","--initialize-insecure","--datadir="+str(work/"data")],timeout=60)
if init.returncode: raise RuntimeError(init.stderr.decode(errors="replace"))
with socket.socket() as probe:
 probe.bind(("127.0.0.1",0));port=probe.getsockname()[1]
log=(work/"mysql.log").open("wb")
server=subprocess.Popen([str(BIN/"mysqld.exe"),"--no-defaults","--datadir="+str(work/"data"),"--port="+str(port),"--bind-address=127.0.0.1","--mysqlx=OFF","--skip-log-bin","--console"],stdout=log,stderr=log,creationflags=flags)
client=[BIN/"mysql.exe","--no-defaults","--protocol=TCP","--host=127.0.0.1","--port="+str(port),"--user=root","--default-character-set=utf8mb4","--batch","--skip-column-names"]
def sql(query):
 result=run(client,input=query.encode("utf-8"),timeout=20)
 if result.returncode: raise RuntimeError(result.stderr.decode(errors="replace"))
 return result.stdout.decode("utf-8").strip()
try:
 for i in range(100):
  if server.poll() is not None: raise RuntimeError("Temporary MySQL exited")
  try:sql("SELECT 1;");break
  except RuntimeError:time.sleep(.2)
 else:raise RuntimeError("Temporary MySQL startup timeout")
 framework=(ROOT/"SQL/ry_20260417.sql").read_text("utf-8")
 bootstrap=["CREATE DATABASE dachang CHARACTER SET utf8mb4;USE dachang;"]
 for table in ["sys_dict_type","sys_dict_data","sys_menu"]:
  bootstrap.append(re.search(r"create table "+table+r"\s*\([\s\S]*?;",framework,re.I).group(0))
 sql("\n".join(bootstrap))
 sql((ROOT/"SQL/fire_place_init.sql").read_text("utf-8"))
 access=(ROOT/"SQL/fire_device_access.sql").read_text("utf-8")
 sql(access);sql(access)
 assert sql("USE dachang;SELECT COUNT(*) FROM fire_place;")=="102"
 assert sql("USE dachang;SELECT COUNT(*) FROM fire_gateway;")=="1"
 # Obtain the exact compiler dependency path from the already-resolved Maven reactor.
 build=run([r"D:\env\apache-maven-3.9.9\bin\mvn.cmd","-o","-X","-pl","ruoyi-admin","-am","-DskipTests","compile"],cwd=ROOT/"RuoYi-Vue-master",timeout=60)
 if build.returncode:raise RuntimeError("Maven compiler dependency discovery failed")
 logtext=build.stdout.decode(errors="replace")
 paths=re.findall(r"compilePath = \[([^\r\n]+)\]",logtext)
 if not paths:raise RuntimeError("No Maven compilePath found")
 cp=os.pathsep.join(paths[-1].split(", "))
 classes=work/"classes";classes.mkdir()
 source=ROOT/"tools/device-capture/java/FireAccessIntegrationTest.java"
 compiled=run(["javac","-encoding","UTF-8","-cp",cp,"-d",classes,source],timeout=30)
 if compiled.returncode:raise RuntimeError(compiled.stderr.decode(errors="replace"))
 tested=run(["java","-Dfile.encoding=UTF-8","-cp",str(classes)+os.pathsep+cp,"FireAccessIntegrationTest",port,ROOT],timeout=45)
 print(tested.stdout.decode(errors="replace"))
 if tested.returncode:raise RuntimeError(tested.stderr.decode(errors="replace"))
 lifecycle=ROOT/"tools/device-capture/java/SseLifecycleTest.java"
 compiled=run(["javac","-encoding","UTF-8","-cp",cp,"-d",classes,lifecycle],timeout=30)
 if compiled.returncode:raise RuntimeError(compiled.stderr.decode(errors="replace"))
 lifecycle_test=run(["java","-Dfile.encoding=UTF-8","-cp",str(classes)+os.pathsep+cp,"SseLifecycleTest"],timeout=20)
 print(lifecycle_test.stdout.decode(errors="replace"))
 if lifecycle_test.returncode:raise RuntimeError(lifecycle_test.stderr.decode(errors="replace"))
 report=dict(isolated_mysql=True,business_database_accessed=False,sql_repeat_safe=True,places=102,gateways=1,
             sse_lifecycle=lifecycle_test.stdout.decode(errors="replace").splitlines()[-1],capture_frames=8,verification="FIRE_ACCESS_INTEGRATION_OK",output=tested.stdout.decode(errors="replace").splitlines()[-1])
 out=ROOT/"docs/verification/fire-device-integration.json";out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print("Verified. Temporary database: "+str(work))
finally:
 try:run([BIN/"mysqladmin.exe","--no-defaults","--protocol=TCP","--host=127.0.0.1","--port="+str(port),"--user=root","shutdown"],timeout=10)
 finally:
  try:server.wait(timeout=10)
  except subprocess.TimeoutExpired:server.terminate();server.wait(timeout=5)
  log.close()
