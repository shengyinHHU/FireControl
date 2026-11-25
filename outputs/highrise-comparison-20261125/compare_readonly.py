import collections
import difflib
import json
import os
from pathlib import Path
import re
import subprocess
import unicodedata
import openpyxl

ROOT=Path(__file__).resolve().parents[2]
config=(ROOT/'RuoYi-Vue-master/ruoyi-admin/src/main/resources/application-druid.yml').read_text(encoding='utf-8')
section=config.split('master:',1)[1].split('slave:',1)[0]
master={k:re.search(r'(?m)^\s*'+k+r':\s*(.*?)\s*$',section)[1].strip('\"\x27') for k in ['url','username','password']}
match=re.search(r'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)',master['url'])
sql="""
START TRANSACTION READ ONLY;
SELECT JSON_OBJECT('id',place_id,'name',name,'type',place_type,'status',status,'deleted',del_flag,'address',address,'community',community_name,'management',management_company,'highrise_count',highrise_building_count,'lng',longitude,'lat',latitude,'crs',coordinate_system,'remark',remark) FROM fire_place ORDER BY place_id;
SELECT JSON_OBJECT('audit',TRUE,'file',b.file_name,'name',j.name,'result',j.status,'reason',j.reason,'matches',j.matches) FROM fire_place_import_batch b JOIN JSON_TABLE(b.result_json,'$.rows[*]' COLUMNS(name VARCHAR(255) PATH '$.name',status VARCHAR(40) PATH '$.status',reason VARCHAR(1000) PATH '$.reason',matches JSON PATH '$.matches')) j WHERE b.state='complete' AND (j.name LIKE '%新华二村%' OR j.name LIKE '%扬子%');
ROLLBACK;
"""
p=subprocess.run(['C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe','--no-defaults','--protocol=TCP','--host='+match[1],'--port='+(match[2] or '3306'),'--user='+str(master['username']),'--database='+match[3],'--default-character-set=utf8mb4','--batch','--raw','--skip-column-names','--connect-timeout=5'],env=dict(os.environ,MYSQL_PWD=str(master['password'])),input=sql,text=True,encoding='utf-8',capture_output=True,timeout=30,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
if p.returncode:raise RuntimeError(p.stderr.replace(str(master['password']),'[REDACTED]'))
query_rows=[json.loads(line) for line in p.stdout.splitlines() if line.startswith('{')]
places=[p for p in query_rows if 'id' in p]
audit=[p for p in query_rows if p.get('audit')]
sheet=openpyxl.load_workbook(ROOT/'docs/2025年江北新区直管区住宅小区物业管理基础数据汇总1.21.xlsx',data_only=True)['大厂']
def norm(s):return re.sub(r'\s+','',unicodedata.normalize('NFKC',str(s or '')))
def key(s):return re.sub(r'[^\w\u4e00-\u9fff]','',norm(s))
source=[]
for r in range(2,sheet.max_row+1):
    v=[sheet.cell(r,c).value for c in range(1,29)]
    if not v[3]:continue
    s=dict(row=r,serial=v[0],name=v[3],community=v[2],address=v[5],lowrise_count=v[13],highrise_count=v[14],elevators=v[15],management=v[25],person=v[26],phone=v[27])
    matches=[p for p in places if key(p['name'])==key(s['name'])]
    s['matches']=matches
    if not matches:
        candidates=[]
        for p in places:
            a,b=key(s['name']),key(p['name'])
            score=difflib.SequenceMatcher(None,a,b).ratio()
            address_same=bool(s['address'] and key(s['address'])==key(p['address']))
            if score>=.4 or address_same:
                candidates.append(dict(score=round(score,3),address_same=address_same,**p))
        candidates.sort(key=lambda p:(p['address_same'],p['score']),reverse=True)
        s['candidates']=candidates[:4]
    source.append(s)
active=[p for p in places if p['type']=='highrise' and p['status']=='0' and p['deleted']=='0']
aliases={24:26,25:27,36:287,37:288,38:289,56:29,72:33,78:307,81:310}
for s in source:
    if not s['matches'] and s['row'] in aliases:
        s['matches']=[p for p in active if p['id']==aliases[s['row']]]
        s['match_method']='名称补充说明或栋幢写法不同，人工核对'
unmatched=[s for s in source if not any(p['type']=='highrise' and p['status']=='0' and p['deleted']=='0' for p in s['matches'])]
matched_ids={p['id'] for s in source for p in s['matches']}
extra=[p for p in active if p['id'] not in matched_ids]
count_changes=[dict(row=s['row'],name=s['name'],source_count=s['highrise_count'],database_count=p['highrise_count'],id=p['id']) for s in source for p in s['matches'] if p['highrise_count'] is not None and p['highrise_count']!=s['highrise_count']]
report=dict(database=match[3],source_rows=len(source),source_unique=len({key(s['name']) for s in source}),database_highrise_rows=len(active),source_highrise_positive=sum(isinstance(s['highrise_count'],(int,float)) and s['highrise_count']>0 for s in source),source_highrise_zero=sum(s['highrise_count']==0 for s in source),matched_source_rows=len(source)-len(unmatched),missing_source_rows=len(unmatched),missing_highrise_positive=sum(isinstance(s['highrise_count'],(int,float)) and s['highrise_count']>0 for s in unmatched),database_places=places,source=source,unmatched=unmatched,extra_records=extra,highrise_count_changes=count_changes,import_audit=audit)
Path(__file__).with_name('comparison.json').write_text(json.dumps(report,ensure_ascii=False,indent=2,default=str),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k not in ['database_places','source','unmatched']},ensure_ascii=False,indent=2))
print('MISSING')
for s in unmatched:
    print(json.dumps({k:v for k,v in s.items() if k not in ['matches','candidates']},ensure_ascii=False,default=str))
