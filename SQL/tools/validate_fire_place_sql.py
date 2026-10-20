"""Validate seed SQL in a disposable isolated MySQL instance; never connects to project DB."""
import sys,os,subprocess,tempfile,time,socket,re,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BIN=Path(r'C:\Program Files\MySQL\MySQL Server 8.0\bin')
work=Path(tempfile.mkdtemp(prefix='firecontrol-mysql-verify-'))
data=work/'data'
flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
def run(args,**kwargs):
 return subprocess.run([str(x) for x in args],creationflags=flags,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kwargs)
init=run([BIN/'mysqld.exe','--no-defaults','--initialize-insecure','--datadir='+str(data)],timeout=90)
if init.returncode: raise RuntimeError(init.stderr.decode(errors='replace'))
s=socket.socket();s.bind(('127.0.0.1',0));port=s.getsockname()[1];s.close()
log=(work/'server.log').open('wb')
server=subprocess.Popen([str(BIN/'mysqld.exe'),'--no-defaults','--datadir='+str(data),'--port='+str(port),'--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin','--console'],creationflags=flags,stdout=log,stderr=log)
client=[BIN/'mysql.exe','--no-defaults','--protocol=TCP','--host=127.0.0.1','--port='+str(port),'--user=root','--default-character-set=utf8mb4','--batch','--raw','--skip-column-names']
def sql(query):
 result=run(client,input=query.encode('utf-8'),timeout=60)
 if result.returncode: raise RuntimeError(result.stderr.decode('utf-8',errors='replace'))
 return result.stdout.decode('utf-8').strip()
try:
 for i in range(100):
  if server.poll() is not None: raise RuntimeError('Temporary mysqld exited: '+(work/'server.log').read_text(errors='replace'))
  try: sql('SELECT 1;');break
  except RuntimeError: time.sleep(.2)
 else: raise RuntimeError('Temporary MySQL startup timed out')
 framework=(ROOT/'SQL/ry_20260417.sql').read_text('utf-8')
 bootstrap=['CREATE DATABASE dachang CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;','USE dachang;']
 for table in ['sys_dict_type','sys_dict_data']:
  bootstrap.append(re.search(r'create table '+table+r'\s*\([\s\S]*?;',framework,re.I).group(0))
  bootstrap.extend(re.findall(r'insert into '+table+r' values\([^\n]*;',framework,re.I))
 sql('\n'.join(bootstrap))
 seed=(ROOT/'SQL/fire_place_init.sql').read_text('utf-8')
 sql(seed)
 counts=sql('USE dachang; SELECT (SELECT COUNT(*) FROM fire_place),(SELECT COUNT(*) FROM fire_place_contact),(SELECT COUNT(*) FROM fire_place_import_row),(SELECT COUNT(*) FROM sys_dict_type WHERE dict_type LIKE \'fire_%\'),(SELECT COUNT(*) FROM sys_dict_data WHERE dict_type LIKE \'fire_%\');')
 assert counts=='101\t202\t101\t3\t11', counts
 assert sql("USE dachang; SELECT COUNT(*) FROM fire_place WHERE longitude IS NOT NULL OR latitude IS NOT NULL OR location_status <> 'pending' OR dept_id IS NOT NULL OR supervisor_user_id IS NOT NULL;")=='0'
 assert sql('USE dachang; SELECT COUNT(*) FROM fire_place_contact c LEFT JOIN fire_place p ON p.place_id=c.place_id WHERE p.place_id IS NULL;')=='0'
 assert sql('USE dachang; SELECT COUNT(*) FROM fire_place_import_row WHERE JSON_VALID(raw_data)=0 OR place_id IS NULL;')=='0'
 # Round-trip all original cell values, duplicate headers, titles, and columns.
 sys.path.insert(0,os.environ['FIRE_SHEET_DEPS'])
 import xlrd,openpyxl
 out=sql('USE dachang; SELECT JSON_OBJECT(\'file\',file_name,\'sheet\',sheet_name,\'row\',`row_number`,\'raw\',raw_data) FROM fire_place_import_row;')
 sources={}
 for line in out.splitlines():
  item=json.loads(line);name=item['file']
  if name not in sources:
   p=ROOT/'docs'/name
   if p.suffix=='.xlsx':
    w=openpyxl.load_workbook(p,data_only=True);sources[name]={s.title:list(s.values) for s in w}
   else:
    w=xlrd.open_workbook(str(p));sources[name]={s.name:[s.row_values(i) for i in range(s.nrows)] for s in w.sheets()}
  rows=sources[name][item['sheet']];row=rows[item['row']-1];headers=rows[1]
  assert item['raw']=={'title':list(rows[0]),'cells':[{'column':i+1,'header':headers[i] if i<len(headers) else None,'value':v} for i,v in enumerate(row)]}, (name,item['row'])
 # Simulate a human edit; rerun in alternate escaping mode and verify no duplicates/overwrites.
 sql("USE dachang; UPDATE fire_place SET name='验证人工修改保留' WHERE place_id=1; UPDATE fire_place_contact SET phone='manual-check' WHERE contact_id=1;")
 sql("SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_BACKSLASH_ESCAPES';\n"+seed)
 assert sql('USE dachang; SELECT (SELECT COUNT(*) FROM fire_place),(SELECT COUNT(*) FROM fire_place_contact),(SELECT COUNT(*) FROM fire_place_import_row);')=='101\t202\t101'
 assert sql('USE dachang; SELECT name FROM fire_place WHERE place_id=1;')=='验证人工修改保留'
 assert sql('USE dachang; SELECT phone FROM fire_place_contact WHERE contact_id=1;')=='manual-check'
 version=sql('SELECT VERSION();')
 report={'engine':'MySQL '+version,'isolated_instance':True,'project_database_accessed':False,'first_import_counts':{'places':101,'contacts':202,'source_rows':101,'new_dict_types':3,'new_dict_values':11},'raw_cells_roundtrip':'101/101 exact','repeat_execution':'passed; no duplicates; manual edits preserved','alternate_sql_mode':'STRICT_TRANS_TABLES,NO_BACKSLASH_ESCAPES passed','coordinate_and_relation_checks':'passed','sql_sha256':hashlib.sha256(seed.encode()).hexdigest()}
 (ROOT/'SQL/fire_place_validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps(report,ensure_ascii=False))
finally:
 run([BIN/'mysqladmin.exe','--no-defaults','--protocol=TCP','--host=127.0.0.1','--port='+str(port),'--user=root','shutdown'],timeout=20)
 try: server.wait(timeout=20)
 except subprocess.TimeoutExpired: server.terminate();server.wait(timeout=10)
 log.close()