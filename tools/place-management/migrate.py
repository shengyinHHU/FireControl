"""Back up and install the additive local place-management schema. Defaults to preflight."""
import argparse
import datetime
import gzip
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import pymysql
import yaml

ROOT=Path(__file__).resolve().parents[2]
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    config=ROOT/'RuoYi-Vue-master/ruoyi-admin/target/classes/application-druid.yml'
    master=yaml.safe_load(config.read_text('utf-8'))['spring']['datasource']['druid']['master']
    match=re.match(r'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)',master['url'])
    if not match or match[1] not in ('localhost','127.0.0.1') or match[3]!='dachang':raise RuntimeError('Only the configured local dachang database is supported')
    db=pymysql.connect(host=match[1],port=int(match[2] or 3306),user=master['username'],password=str(master['password']),database=match[3],charset='utf8mb4',cursorclass=pymysql.cursors.DictCursor,connect_timeout=5,read_timeout=30)
    try:
        with db.cursor() as cur:
            snapshot={}
            for table in ('fire_place','fire_place_contact','sys_menu','sys_role_menu'):
                cur.execute(f'SHOW CREATE TABLE `{table}`');ddl=cur.fetchone()['Create Table'];cur.execute(f'SELECT * FROM `{table}`');snapshot[table]={'ddl':ddl,'rows':cur.fetchall()}
        report={'apply':args.apply,'database':match[3],'counts':{table:len(value['rows']) for table,value in snapshot.items()}}
        if args.apply:
            backup=ROOT/'outputs/place-management/backups'/('before-crud-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')+'.json.gz');backup.parent.mkdir(parents=True,exist_ok=True)
            with gzip.open(backup,'wt',encoding='utf-8') as out:json.dump(snapshot,out,ensure_ascii=False,default=str)
            db.commit();env=os.environ.copy();env['MYSQL_PWD']=str(master['password'])
            result=subprocess.run([r'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe','--no-defaults','--protocol=TCP','--host='+match[1],'--port='+str(match[2] or 3306),'--user='+master['username'],'--default-character-set=utf8mb4'],env=env,input=(ROOT/'SQL/fire_place_crud_upgrade.sql').read_bytes(),stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=45,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
            if result.returncode:raise RuntimeError(result.stderr.decode(errors='replace'))
            with db.cursor() as cur:
                for table in ('fire_place','fire_place_contact','sys_role_menu'):
                    cur.execute(f'SELECT * FROM `{table}`');after=cur.fetchall();before=snapshot[table]['rows']
                    clean=lambda rows:sorted(json.dumps({k:v for k,v in row.items() if k!='revision'},ensure_ascii=False,sort_keys=True,default=str) for row in rows)
                    assert clean(before)==clean(after),table+' data changed unexpectedly'
                cur.execute("SELECT COUNT(*) AS n FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_place' AND column_name='revision'");assert cur.fetchone()['n']==1
            report.update(completed=True,backup=str(backup),businessRecordsUnchanged=True,roleGrantsUnchanged=True)
            output=ROOT/'outputs/place-management/migration.json';output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf-8')
        print(json.dumps(report,ensure_ascii=False))
    finally:db.close()
if __name__=='__main__':
    sys.stdout.reconfigure(encoding='utf-8');main()
