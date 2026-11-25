import collections
import difflib
import json
import os
from pathlib import Path
import re
import subprocess
import unicodedata

import openpyxl

ROOT = Path(__file__).resolve().parents[2]
config_text = (ROOT / 'RuoYi-Vue-master/ruoyi-admin/src/main/resources/application-druid.yml').read_text(encoding='utf-8')
master_text = config_text.split('master:',1)[1].split('slave:',1)[0]
master = {key:re.search(r'(?m)^\s*'+key+r':\s*(.*?)\s*$',master_text)[1].strip('\"\x27') for key in ['url','username','password']}
match = re.search(r'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)', master['url'])
env = dict(os.environ, MYSQL_PWD=str(master['password']))
sql = """
START TRANSACTION READ ONLY;
SELECT JSON_OBJECT('place_id',place_id,'name',name,'place_type',place_type,'industry_category',industry_category,'status',status,'del_flag',del_flag,'longitude',longitude,'latitude',latitude,'coordinate_system',coordinate_system,'remark',remark) FROM fire_place ORDER BY place_id;
SELECT JSON_OBJECT('contact_place_id',place_id,'person',contact_name,'phone',phone,'status',status,'del_flag',del_flag) FROM fire_place_contact;
ROLLBACK;
"""
proc = subprocess.run([
    'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe', '--no-defaults',
    '--protocol=TCP', '--host='+match[1], '--port='+(match[2] or '3306'),
    '--user='+str(master['username']), '--database='+match[3],
    '--default-character-set=utf8mb4', '--batch', '--raw', '--skip-column-names',
    '--connect-timeout=5',
], env=env, input=sql, text=True, encoding='utf-8', capture_output=True, timeout=30,
   creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
if proc.returncode:
    raise RuntimeError(proc.stderr.replace(str(master['password']), '[REDACTED]'))
data = [json.loads(line) for line in proc.stdout.splitlines() if line.startswith('{')]
places = [r for r in data if 'place_id' in r]
contacts = collections.defaultdict(list)
for r in data:
    if 'contact_place_id' in r and r['status']=='0' and r['del_flag']=='0':
        contacts[r['contact_place_id']].append({'person':r['person'],'phone':r['phone']})
for p in places:
    p['contacts']=contacts[p['place_id']]

def norm(value):
    return re.sub(r'\s+', '', unicodedata.normalize('NFKC', str(value or '')))

sheet = openpyxl.load_workbook(ROOT/'docs/花名册张贴企业面.xlsx', data_only=True).worksheets[0]
source=[]
for c in [1,6,11,16]:
    cat=None
    for r in range(3,sheet.max_row+1):
        v=[sheet.cell(r,j).value for j in range(c,c+5)]
        if v[1] is not None: cat=v[1]
        if v[2] is not None:
            source.append({'name':str(v[2]).strip(),'serial':v[0],'category':cat,'person':v[3],'phone':str(v[4] or '').strip(),'cell':sheet.cell(r,c+2).coordinate})

active=[p for p in places if p['place_type']=='industry' and p['status']=='0' and p['del_flag']=='0']
active_names={norm(p['name']) for p in active}
source_names={norm(p['name']) for p in source}
missing=[]
for s in source:
    if norm(s['name']) in active_names:continue
    scores=[]
    for p in places:
        a,b=norm(s['name']),norm(p['name'])
        score=difflib.SequenceMatcher(None,a,b).ratio()
        same_phone=any(norm(c['phone'])==norm(s['phone']) and s['phone'] for c in p['contacts'])
        if score>=0.45 or same_phone:
            scores.append({'score':round(score,3),'same_phone':bool(same_phone),**p})
    scores.sort(key=lambda p:(p['same_phone'],p['score']),reverse=True)
    missing.append({**s,'candidates':scores[:4]})
extra=[p for p in active if norm(p['name']) not in source_names]
duplicates=[]
for name,count in collections.Counter(norm(s['name']) for s in source).items():
    if count>1: duplicates.append({'name':name,'count':count,'source':[s for s in source if norm(s['name'])==name],'database':[p for p in active if norm(p['name'])==name]})
def company_key(value):
    value=norm(value).lower()
    value=re.sub(r'[（(][^（）()]*[）)]','',value)
    value=re.sub(r'^(江苏省|江苏|南京市|南京)','',value)
    return re.sub(r'(有限责任公司|股份有限公司|有限公司)$','',value)

matched=[]
for s in source:
    matches=[p for p in active if norm(p['name'])==norm(s['name'])]
    method='原名一致'
    if not matches:
        matches=[p for p in active if company_key(p['name'])==company_key(s['name'])]
        method='去除地区及公司后缀后名称一致'
    if not matches and s['name']=='南京卓泽化工设备有':
        matches=[p for p in active if p['name']=='南京卓泽化工设备有限公司']
        method='原表名称截断，按主体名称对应'
    matched.append({**s,'method':method if matches else '未匹配','matches':[{'id':p['place_id'],'name':p['name']} for p in matches]})
matched_ids={p['id'] for s in matched for p in s['matches']}
groups=collections.defaultdict(list)
for s in matched:
    for p in s['matches']:groups[p['id']].append(s)
report={
    'database':match[3], 'source_sheet':sheet.title,
    'source_rows':len(source),'source_unique':len(source_names),
    'database_counts':[{'type':k[0],'status':k[1],'del_flag':k[2],'count':v} for k,v in collections.Counter((p['place_type'],p['status'],p['del_flag']) for p in places).items()],
    'active_industry_rows':len(active),'active_industry_unique':len(active_names),
    'map_industry_rows':sum(p['coordinate_system']=='GCJ02' and p['longitude'] is not None and p['latitude'] is not None and -180<=p['longitude']<=180 and -90<=p['latitude']<=90 and (p['longitude']!=0 or p['latitude']!=0) for p in active),
    'missing_exact_name':missing,'extra_exact_name':extra,'source_duplicates':duplicates,
    'matched_rows':matched,
    'unmatched_rows':[s for s in matched if not s['matches']],
    'matched_database_count':len(matched_ids),
    'extra_database_companies':[p for p in active if p['place_id'] not in matched_ids],
    'repeated_source_companies':[{'id':i,'name':g[0]['matches'][0]['name'],'rows':[{'name':s['name'],'cell':s['cell']} for s in g]} for i,g in groups.items() if len(g)>1],
}
Path(__file__).with_name('comparison.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k not in ['missing_exact_name','extra_exact_name','source_duplicates','matched_rows']},ensure_ascii=False,indent=2))
