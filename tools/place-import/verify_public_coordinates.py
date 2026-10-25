"""Independent cell, cache, boundary and preservation checks for public-address output."""
import collections,hashlib,importlib.util,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];out=ROOT/'outputs/regulatory-geocoded'
def mod(n,p):
 s=importlib.util.spec_from_file_location(n,p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
c=mod('c',ROOT/'tools/place-import/convert_lists.py');f=mod('f',ROOT/'tools/place-import/fuzzy_dachang.py')
d=json.loads((out/'public_writeback.json').read_text(encoding='utf-8'));_,before=c.read_book(Path(d['input']));_,after=c.read_book(Path(d['output']));boundary=json.loads((ROOT/'RuoYi-Vue3/src/assets/geo/dachang-boundary.json').read_text(encoding='utf-8'))['geometry']
assert hashlib.sha256(Path(d['input']).read_bytes()).hexdigest()==d['sha256']
changes={r['excel_row']for r in d['rows']if r['chosen']}
for k,v in before.items():
 col=''.join(x for x in k if x.isalpha());row=int(''.join(x for x in k if x.isdigit()))
 if row in changes and col in ('P','Q','R','Y','Z','AA','AB','AC','AD','AE'):continue
 assert after.get(k,'')==v,(k,v,after.get(k))
for r in d['rows']:
 i=r['excel_row'];p=r['chosen']
 if p:
  assert p in r['candidates']and f.within(p,boundary)
  assert abs(float(after[f'P{i}'])-p['longitude'])<1e-7 and abs(float(after[f'Q{i}'])-p['latitude'])<1e-7
 else:assert before.get(f'P{i}','')==after.get(f'P{i}','')and before.get(f'Q{i}','')==after.get(f'Q{i}','')
 for col,value in zip(['AF','AG','AH','AI','AJ','AK','AL','AM','AN','AO','AP'],r['values']):
  actual=after.get(f'{col}{i}','');assert c.clean(value)==actual,(i,col)
assert sum(bool(after.get(f'P{i}'))and bool(after.get(f'Q{i}'))for i in range(2,327))==187
report={**d['summary'],'status':'PASS','original_user_fields_and_173_coordinates_preserved':True,'new_points_inside_dachang':True,'evidence_cells_verified':True,'unresolved_with_address_leads':sum(bool(r['source'])and not r['chosen']for r in d['rows']),'unresolved_without_verified_address':sum(not r['source']for r in d['rows'])}
(out/'public_validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(report,ensure_ascii=False))
