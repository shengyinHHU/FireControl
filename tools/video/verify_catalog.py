"""Catalog integration verification using isolated MySQL; no business credentials or vendor calls."""
import os,sys,subprocess,tempfile,time,socket,re,json
from pathlib import Path
sys.stdout.reconfigure(encoding="utf-8",errors="replace")
ROOT=Path(__file__).resolve().parents[2]
BIN=Path(r"C:\Program Files\MySQL\MySQL Server 8.0\bin")
work=Path(tempfile.mkdtemp(prefix="video-catalog-integration-"))
flags=getattr(subprocess,"CREATE_NO_WINDOW",0)
def run(args,**kw):return subprocess.run([str(x) for x in args],creationflags=flags,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kw)
r=run([BIN/"mysqld.exe","--no-defaults","--initialize-insecure","--datadir="+str(work/"data")],timeout=60)
if r.returncode:raise RuntimeError(r.stderr.decode(errors="replace"))
with socket.socket() as probe:probe.bind(("127.0.0.1",0));port=probe.getsockname()[1]
log=(work/"mysql.log").open("wb")
server=subprocess.Popen([str(BIN/"mysqld.exe"),"--no-defaults","--datadir="+str(work/"data"),"--port="+str(port),"--bind-address=127.0.0.1","--mysqlx=OFF","--skip-log-bin","--console"],stdout=log,stderr=log,creationflags=flags)
client=[BIN/"mysql.exe","--no-defaults","--protocol=TCP","--host=127.0.0.1","--port="+str(port),"--user=root","--default-character-set=utf8mb4","--batch","--skip-column-names"]
def sql(q):
 r=run(client,input=q.encode("utf-8"),timeout=20)
 if r.returncode:raise RuntimeError(r.stderr.decode(errors="replace"))
 return r.stdout.decode("utf-8").strip()
try:
 for i in range(100):
  try:sql("SELECT 1");break
  except RuntimeError:time.sleep(.2)
 framework=(ROOT/"SQL/ry_20260417.sql").read_text("utf-8")
 sql("CREATE DATABASE dachang CHARACTER SET utf8mb4;USE dachang;"+re.search(r"create table sys_menu\s*\([\s\S]*?;",framework,re.I).group(0))
 migration=(ROOT/"SQL/fire_video_catalog.sql").read_text("utf-8");sql(migration);sql(migration)
 b=run([r"D:\env\apache-maven-3.9.9\bin\mvn.cmd","-o","-X","-pl","ruoyi-admin","-am","-DskipTests","compile"],cwd=ROOT/"RuoYi-Vue-master",timeout=90)
 if b.returncode:raise RuntimeError(b.stdout.decode(errors="replace")[-5000:])
 paths=re.findall(r"compilePath = \[([^\r\n]+)\]",b.stdout.decode(errors="replace"));cp=os.pathsep.join(paths[-1].split(", "))
 classes=work/"classes";classes.mkdir()
 c=run(["javac","-encoding","UTF-8","-cp",cp,"-d",classes,ROOT/"tools/video/VideoCatalogIntegrationTest.java"],timeout=30)
 if c.returncode:raise RuntimeError(c.stderr.decode(errors="replace"))
 t=run(["java","-Dfile.encoding=UTF-8","-cp",str(classes)+os.pathsep+cp,"VideoCatalogIntegrationTest",port],timeout=90)
 print(t.stdout.decode(errors="replace"))
 if t.returncode:raise RuntimeError(t.stderr.decode(errors="replace"))
 out=ROOT/"docs/verification/video-catalog-integration.json";out.write_text(json.dumps(dict(isolated_mysql=True,fixture_vendor=True,business_database_accessed=False,external_api_accessed=False,migration_repeat_safe=True,result=t.stdout.decode(errors="replace").strip()),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
finally:
 run([BIN/"mysqladmin.exe","--no-defaults","--protocol=TCP","--host=127.0.0.1","--port="+str(port),"--user=root","shutdown"],timeout=10)
 try:server.wait(timeout=10)
 except subprocess.TimeoutExpired:server.terminate();server.wait(timeout=5)
 log.close()
