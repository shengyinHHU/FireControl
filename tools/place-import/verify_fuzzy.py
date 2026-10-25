"""Verify fuzzy output against original cells and true cached candidates; export audit CSV."""
import collections,csv,hashlib,importlib.util,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];out=ROOT/'outputs/regulatory-geocoded'
def module(name,file):
 s=importlib.util.spec_from_file_location(name,file);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
c=module('c',ROOT/'tools/place-import/convert_lists.py');fuzzy=module('f',ROOT/'tools/place-import/fuzzy_dachang.py')
d=json.loads((out/'fuzzy_writeback.json').read_text(encoding='utf-8'));details=json.loads((out/'fuzzy_results.json').read_text(encoding='utf-8'));boundary=json.loads((ROOT/'RuoYi-Vue3/src/assets/geo/dachang-boundary.json').read_text(encoding='utf-8'))
assert hashlib.sha256(Path(d['input']).read_bytes()).hexdigest()==d['sha256']
_,before=c.read_book(Path(d['input']));_,after=c.read_book(Path(d['output']))
for k,v in before.items():
 if ''.join(x for x in k if x.isalpha())not in ('P','Q','R'):assert after.get(k,'')==v,(k,v,after.get(k))
for row,detail in zip(d['rows'],details):
 i=row['excel_row'];p=row['selected']
 if p:
  assert fuzzy.within(p,boundary['geometry'])
  assert fuzzy.score(row['name'],p['name'])>=.65
  assert p in detail['candidates']
  assert abs(float(after[f'P{i}'])-p['longitude'])<1e-7 and abs(float(after[f'Q{i}'])-p['latitude'])<1e-7 and after[f'R{i}']=='GCJ02'
 else:assert not after.get(f'P{i}','')and not after.get(f'Q{i}','')
with (out/'大厂街道定位审查.csv').open('w',encoding='utf-8-sig',newline='')as file:
 w=csv.writer(file);w.writerow(['原表行号','原对象名称','选择的高德名称','经度','纬度','高德地址','相似度','判断依据'])
 for row in d['rows']:
  p=row['selected']or{};w.writerow([row['excel_row'],row['name'],p.get('name',''),p.get('longitude',''),p.get('latitude',''),p.get('matched_address',''),p.get('similarity',''),row['reason']])
counts=collections.Counter(x['reason'].split('，')[0]for x in d['rows']if not x['selected'])
report=dict(total=len(d['rows']),selected=sum(bool(x['selected'])for x in d['rows']),unresolved=sum(not x['selected']for x in d['rows']),unresolved_reasons=dict(counts),verification='PASS: original cells preserved, source unchanged, selected points inside confirmed polygon and match real candidates')
(out/'fuzzy_validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(report,ensure_ascii=False))
