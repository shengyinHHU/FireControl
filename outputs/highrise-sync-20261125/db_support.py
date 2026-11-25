"""Local database access; never print credentials."""
import json
import re
from pathlib import Path
import pymysql
import yaml

ROOT=Path(__file__).resolve().parents[2]
OUT=Path(__file__).resolve().parent

def connect():
    config=yaml.safe_load((ROOT/'RuoYi-Vue-master/ruoyi-admin/src/main/resources/application-druid.yml').read_text(encoding='utf-8'))
    cfg=config['spring']['datasource']['druid']['master']
    m=re.match(r'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)',cfg['url'])
    assert m and m[1] in ['localhost','127.0.0.1'] and m[3]=='dachang','Unexpected database'
    return pymysql.connect(host=m[1],port=int(m[2] or 3306),user=cfg['username'],password=str(cfg['password']),database=m[3],charset='utf8mb4',cursorclass=pymysql.cursors.DictCursor,connect_timeout=5,read_timeout=60,write_timeout=60,autocommit=False)

def save(name,data):
    (OUT/name).write_text(json.dumps(data,ensure_ascii=False,indent=2,default=str),encoding='utf-8')

if __name__=='__main__':
    db=connect()
    try:
        with db.cursor() as cur:
            cur.execute('START TRANSACTION READ ONLY')
            cur.execute("SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND (COLUMN_NAME LIKE '%place%id%' OR TABLE_NAME IN ('fire_place','fire_place_contact','fire_place_import_row')) ORDER BY TABLE_NAME,ORDINAL_POSITION")
            cols=cur.fetchall();save('columns.json',cols)
            tables={r['TABLE_NAME'] for r in cols}
            schemas={};refs={};highrise=[]
            for table in sorted(tables):
                cur.execute('SHOW CREATE TABLE `'+table+'`');schemas[table]=cur.fetchone()['Create Table']
                if table not in ['fire_place','fire_place_contact','fire_place_import_row']:
                    for c in [r['COLUMN_NAME'] for r in cols if r['TABLE_NAME']==table and 'place' in r['COLUMN_NAME']]:
                        cur.execute('SELECT `'+c+'` AS place_id,COUNT(*) AS n FROM `'+table+'` WHERE `'+c+'` IN (12,15,39,41,268,274,325,326,327) GROUP BY `'+c+'`')
                        refs[table+'.'+c]=cur.fetchall()
            cur.execute("SELECT * FROM fire_place WHERE place_type='highrise' ORDER BY place_id");highrise=cur.fetchall()
            cur.execute('SELECT * FROM fire_place_contact WHERE place_id IN (12,15,39,41,268,274,325,326,327) ORDER BY place_id,contact_id');contacts=cur.fetchall()
            save('preflight.json',dict(schemas=schemas,references=refs,highrise=highrise,contacts=contacts))
            print(json.dumps(dict(tables=sorted(tables),references=refs,highrise_count=len(highrise),schemas={k:v for k,v in schemas.items() if k in ['fire_place_contact','fire_place_import_row']}),ensure_ascii=False,default=str))
        db.rollback()
    finally:db.close()
