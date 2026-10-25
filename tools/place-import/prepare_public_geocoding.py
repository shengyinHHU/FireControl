import hashlib,json
from pathlib import Path
out=Path('outputs/regulatory-geocoded');cache=json.loads((out/'cache.json').read_text(encoding='utf-8'));sources=json.loads((out/'reviewed_public_addresses.json').read_text(encoding='utf-8'));tasks=[];seen=set()
for r in sources:
 if not r['query']:continue
 for kind,params in [('geocode',dict(address=r['address'],city='南京市',output='JSON')),('poi',dict(keywords=r['full_name'],city='南京市',citylimit='true',offset='5',page='1',extensions='base',output='JSON'))]:
  fp=hashlib.sha256(json.dumps([kind,params],ensure_ascii=False,sort_keys=True).encode()).hexdigest()
  if fp in cache or fp in seen:continue
  seen.add(fp);tasks.append(dict(kind=kind,params=params,fingerprint=fp,name=r['name'],record_key=r['name']))
(out/'public_address_tasks.json').write_text(json.dumps(tasks,ensure_ascii=False),encoding='utf-8');print('New AMap queries:',len(tasks))
