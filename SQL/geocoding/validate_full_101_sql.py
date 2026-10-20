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
 sql(seed.replace(') ENGINE=InnoDB DEFAULT', ') ENGINE=InnoDB AUTO_INCREMENT=5001 DEFAULT', 1))
 results=json.loads((ROOT/'SQL/geocoding/js_output/full_101/results_all_101.json').read_text('utf-8'))
 selected={r['row']['source_key']:next(c for c in r['candidates'] if c['candidate_id']==r['selected_id']) for r in results if r['selected_id']}
 assert len(results)==101 and len(selected)==101
 protected=next(iter(selected))
 assert sql('USE dachang; SELECT MIN(place_id) FROM fire_place;')=='5001'
 sql("USE dachang; UPDATE fire_place SET longitude=119.1234567,latitude=32.1000000,location_status='confirmed',coordinate_source='manual' WHERE source_key='"+protected+"';")
 update=(ROOT/'SQL/geocoding/js_output/full_101/update_coordinates.sql').read_text('utf-8')
 delta=(ROOT/'SQL/geocoding/js_output/full_101/update_coordinates_additional_31.sql').read_text('utf-8')
 assert sql(delta)=='31\t31'
 output=sql(update)
 assert output=='69\t101',output
 output=sql(update)
 assert output=='0\t101',output
 assert sql("USE dachang; SELECT COUNT(*) FROM fire_place WHERE location_status='review';")=='100'
 assert sql("USE dachang; SELECT COUNT(*) FROM fire_place WHERE location_status='pending' AND longitude IS NULL AND latitude IS NULL;")=='0'
 assert sql("USE dachang; SELECT longitude,coordinate_source FROM fire_place WHERE source_key='"+protected+"';")=='119.1234567\tmanual'
 actual=sql("USE dachang; SELECT JSON_OBJECT('key',source_key,'lng',longitude,'lat',latitude) FROM fire_place WHERE location_status='review';")
 for line in actual.splitlines():
  r=json.loads(line);c=selected[r['key']]
  assert abs(r['lng']-c['longitude'])<0.00000005
  assert abs(r['lat']-c['latitude'])<0.00000005
 assert sql('USE dachang; SELECT COUNT(*) FROM fire_place_contact;')=='202'
 version=sql('SELECT VERSION();')
 report={'engine':'MySQL '+version,'isolated_instance':True,'business_database_accessed':False,'source_records':101,'sql_candidates':101,'additional_sql_candidates':31,'additional_first_run_updates':31,'actual_updates_in_protection_test':100,'existing_confirmed_coordinate_preserved':True,'repeat_execution_updates':0,'unresolved_left_null':0,'source_key_resolves_actual_ids_starting_at':5001,'coordinate_values_match_cached_results':True,'sql_sha256':hashlib.sha256(update.encode()).hexdigest()}
 (ROOT/'SQL/geocoding/js_output/full_101/validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps(report,ensure_ascii=False))
finally:
 run([BIN/'mysqladmin.exe','--no-defaults','--protocol=TCP','--host=127.0.0.1','--port='+str(port),'--user=root','shutdown'],timeout=20)
 try: server.wait(timeout=20)
 except subprocess.TimeoutExpired: server.terminate();server.wait(timeout=10)
 log.close()