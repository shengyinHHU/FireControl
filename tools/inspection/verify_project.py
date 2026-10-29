from pathlib import Path
import os,re,subprocess,tempfile,socket,time,json
ROOT=Path(__file__).resolve().parents[2];BIN=Path(r'C:\Program Files\MySQL\MySQL Server 8.0\bin');work=Path(tempfile.mkdtemp(prefix='fire-inspection-'))
flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
def run(args,**kw):return subprocess.run([str(a) for a in args],creationflags=flags,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kw)
result=run([BIN/'mysqld.exe','--no-defaults','--initialize-insecure','--datadir='+str(work/'data')],timeout=60)
if result.returncode:raise RuntimeError(result.stderr.decode(errors='replace'))
with socket.socket() as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
log=(work/'mysql.log').open('wb');server=subprocess.Popen([str(BIN/'mysqld.exe'),'--no-defaults','--datadir='+str(work/'data'),'--port='+str(port),'--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin','--console'],stdout=log,stderr=log,creationflags=flags)
client=[BIN/'mysql.exe','--no-defaults','--protocol=TCP','--host=127.0.0.1','--port='+str(port),'--user=root','--default-character-set=utf8mb4','--batch','--skip-column-names']
def sql(query):
 result=run(client,input=query.encode('utf-8'),timeout=20)
 if result.returncode:raise RuntimeError(result.stderr.decode(errors='replace'))
 return result.stdout.decode('utf-8').strip()
try:
 for _ in range(100):
  try:sql('SELECT 1;');break
  except RuntimeError:time.sleep(.2)
 else:raise RuntimeError('Temporary MySQL startup timeout')
 framework=(ROOT/'SQL/ry_20260417.sql').read_text('utf-8');sql('CREATE DATABASE dachang CHARACTER SET utf8mb4;USE dachang;'+re.search(r'create table sys_menu\s*\([\s\S]*?;',framework,re.I).group(0))
 sql("USE dachang;CREATE TABLE fire_place(place_id BIGINT PRIMARY KEY,name VARCHAR(255));INSERT INTO fire_place VALUES(1,'测试机构');")
 schema=(ROOT/'SQL/fire_inspection.sql').read_text('utf-8');sql(schema);sql(schema)
 sql("USE dachang;ALTER TABLE fire_inspection_post DROP COLUMN contact_name,DROP COLUMN contact_phone;INSERT INTO fire_inspection_post(post_name,supervisor_user_id,create_by,create_time,update_by,update_time) VALUES('existing contact migration',1,'admin',NOW(),'admin',NOW());")
 upgrade=(ROOT/'SQL/fire_inspection_contact_upgrade.sql').read_text('utf-8');sql(upgrade);sql(upgrade)
 assert sql("USE dachang;SELECT COUNT(*) FROM fire_inspection_post WHERE post_name='existing contact migration' AND contact_name='' AND contact_phone='';")=='1'
 sql('USE dachang;DELETE FROM fire_inspection_post;')
 build=run([r'D:\env\apache-maven-3.9.9\bin\mvn.cmd','-o','-X','-pl','ruoyi-admin','-am','-DskipTests','compile'],cwd=ROOT/'RuoYi-Vue-master',timeout=60)
 if build.returncode:raise RuntimeError(build.stdout.decode(errors='replace')[-2000:])
 paths=re.findall(r'compilePath = \[([^\r\n]+)\]',build.stdout.decode(errors='replace'));cp=os.pathsep.join(paths[-1].split(', '));classes=work/'classes';classes.mkdir()
 compiled=run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,ROOT/'tools/inspection/InspectionIntegrationCheck.java'],timeout=30)
 if compiled.returncode:raise RuntimeError(compiled.stderr.decode(errors='replace'))
 tested=run(['java','-Dfile.encoding=UTF-8','-cp',str(classes)+os.pathsep+cp,'InspectionIntegrationCheck',port,ROOT,work],timeout=60)
 print(tested.stdout.decode('utf-8',errors='replace'))
 if tested.returncode:raise RuntimeError(tested.stderr.decode(errors='replace'))
 (ROOT/'docs/verification/inspection-integration.json').write_text(json.dumps(dict(isolatedMySQL=True,businessDatabaseAccessed=False,vendorApiAccessed=False,schemaRepeatSafe=True,result=tested.stdout.decode('utf-8').strip()),ensure_ascii=False,indent=2),encoding='utf-8')
finally:
 try:run([BIN/'mysqladmin.exe','--no-defaults','--protocol=TCP','--host=127.0.0.1','--port='+str(port),'--user=root','shutdown'],timeout=10)
 finally:
  try:server.wait(timeout=10)
  except subprocess.TimeoutExpired:server.terminate();server.wait(timeout=5)
  log.close()
