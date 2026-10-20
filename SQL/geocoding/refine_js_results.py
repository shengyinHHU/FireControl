"""Offline disambiguation of cached JS API results; never invents coordinates."""
import csv,hashlib,importlib.util,io,json,math,re
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('geo',HERE/'geocode_places.py');g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
OUT=HERE/'js_output'

def distance(a,b):
 x1,y1,x2,y2=map(math.radians,[a['longitude'],a['latitude'],b['longitude'],b['latitude']])
 h=math.sin((y2-y1)/2)**2+math.cos(y1)*math.cos(y2)*math.sin((x2-x1)/2)**2
 return 6371000*2*math.asin(min(1,math.sqrt(h)))

def address_core(value):
 value=value or ''
 for word in ['江苏省','南京市','六合区','江北新区','大厂街道','大厂镇']:
  value=value.replace(word,'')
 if value.startswith('大厂'):value=value[2:]
 return re.sub(r'[\s，,。；;]+','',value)

def aliases(value):
 parts=[value]+re.findall(r'[（(]([^（）()]+)[）)]',value)
 stripped=re.sub(r'^(?:江苏省)?(?:南京市?)?(?:江北新区)?','',value)
 parts.append(stripped)
 return {g.normalize_name(x) for x in parts if x}

def refine(result):
 if result['selected_id']:return result
 row=result['row'];allc=result['candidates'];geo=[c for c in allc if c['method']=='amap_geocode' and c['eligible']]
 core=address_core(row['normalized_address'] or row['address'])
 # Match original house number rather than trusting the API's first returned point.
 matching=[c for c in geo if re.search(r'\d+号',core) and address_core(c['matched_address']).startswith(core)]
 pool=matching or geo
 if len(matching)==1:
  c=matching[0];result['selected_id']=c['candidate_id'];result['reason']='原始道路门牌地址前缀唯一一致；仍需地图核验';return result
 if len(pool)>1:
  addr={address_core(c['matched_address']) for c in pool}
  near=max(distance(a,b) for a in pool for b in pool)<=150
  priority={'单元号':0,'门址':1,'门牌号':2,'住宅区':3,'兴趣点':4}
  best=min(priority.get(c['level'],99) for c in pool)
  top=[c for c in pool if priority.get(c['level'],99)==best]
  if len(addr)==1 and near and len(top)==1:
   result['selected_id']=top[0]['candidate_id'];result['reason']='同一匹配地址的多个精细点相距不超过150米，按匹配级别优先门址；待核验';return result
 # JS PlaceSearch requests were strictly restricted to Nanjing. Preserve this provenance;
 # do not fabricate an official adcode for POIs whose SDK response omitted it.
 candidates=[]
 for c in allc:
  if c['method']!='amap_poi':continue
  same=bool(aliases(c['name']) & aliases(row['name']))
  sameaddr=bool(core and re.search(r'\d+号',core) and address_core(c['matched_address']).startswith(core))
  near=any(distance(c,p)<=150 for p in allc if p['method']=='amap_geocode' and p['in_nanjing'])
  if same and (sameaddr or near):candidates.append((c,near,sameaddr))
 if len(candidates)==1:
  c,near,sameaddr=candidates[0]
  c['in_nanjing']=True;c['eligible']=True;c['region_basis']='JS PlaceSearch city=南京市, citylimit=true; original adcode remains empty if omitted'
  result['selected_id']=c['candidate_id']
  result['reason']='机构名称或表内明确别名一致，且'+('与地址解析点相距不超过150米' if near else '道路门牌地址一致')+'；待核验'
 return result

def main():
 results=json.loads((OUT/'results.json').read_text('utf-8'))
 before=sum(bool(r['selected_id']) for r in results)
 results=[refine(r) for r in results]
 g.atomic_json(OUT/'results.json',results)
 cache=json.loads((OUT/'cache.json').read_text('utf-8'))
 g.export(results,OUT,{'stopped_reason':'','review_mode':False,'coordinate_status':'review','api':'AMap JS API','source':'All 101 institutions; 156 cached queries; offline disambiguation only','selected_before_refinement':before},len(cache))
 # One row per institution, plus a separate list of unresolved institutions.
 fields=['机构名称','类别','原始地址','建议经度','建议纬度','匹配地址','匹配级别','定位来源','候选数量','处理状态','判断依据','source_key']
 all_rows=[]
 for r in results:
  selected=next((c for c in r['candidates'] if c['candidate_id']==r['selected_id']),{})
  all_rows.append(dict(zip(fields,[r['row']['name'],r['row']['place_type'],r['row']['address'],selected.get('longitude',''),selected.get('latitude',''),selected.get('matched_address',''),selected.get('level',''),selected.get('method',''),len(r['candidates']),'待核验候选' if selected else '需人工选择',r['reason'],r['row']['source_key']])))
 for filename,rows in [('机构坐标审查清单.csv',all_rows),('需人工核对的机构.csv',[r for r in all_rows if r['处理状态']=='需人工选择'])]:
  b=io.StringIO(newline='');w=csv.DictWriter(b,fieldnames=fields);w.writeheader();w.writerows(rows);(OUT/filename).write_text(b.getvalue(),encoding='utf-8-sig')
 print(json.dumps({'records':len(results),'selected':sum(bool(r['selected_id']) for r in results),'unresolved':[{'name':r['row']['name'],'address':r['row']['address'],'candidates':len(r['candidates'])} for r in results if not r['selected_id']]},ensure_ascii=False))
if __name__=='__main__':main()