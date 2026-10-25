"""Fuzzy POI matching within the project's confirmed GCJ02 Dachang polygon."""
import argparse,collections,csv,difflib,hashlib,importlib.util,json,math,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'outputs/regulatory-geocoded'
def norm(s):
 s=re.sub(r'[（(][^（）()]*[）)]','',s);s=re.sub(r'^(江苏省|南京市|南京|江苏|江北新区|大厂街道|大厂)+','',s);s=s.replace('南钢','南京钢铁');s=re.sub(r'有限责任公司|股份有限公司|有限公司|集团公司|公司','',s);return re.sub(r'[^\w]','',s)
def score(a,b):
 a,b=norm(a),norm(b)
 if not a or not b:return 0
 if a==b:return 1
 if min(len(a),len(b))<=2:return 0
 # Similarity must not confuse numbered villages or opposing east/west sections.
 for pattern in [r'([一二三四五六七八九十0-9]+)(?=村|幢|栋|区)',r'([东西南北])(?=村|苑)']:
  x,y=re.findall(pattern,a),re.findall(pattern,b)
  if x and y and x!=y:return 0
 if min(len(a),len(b))>=4 and (a in b or b in a):return .9
 return difflib.SequenceMatcher(None,a,b).ratio()
def dist(a,b):
 x,y,u,v=map(math.radians,[a['longitude'],a['latitude'],b['longitude'],b['latitude']]);h=math.sin((v-y)/2)**2+math.cos(y)*math.cos(v)*math.sin((u-x)/2)**2;return 12742000*math.asin(min(1,math.sqrt(h)))
def ring_inside(x,y,ring):
 inside=False
 for (a,b),(c,d)in zip(ring,ring[1:]+ring[:1]):
  if (b>y)!=(d>y) and x<(c-a)*(y-b)/(d-b)+a:inside=not inside
 return inside
def within(c,geo):
 x,y=c['longitude'],c['latitude']
 return any(ring_inside(x,y,p[0])and not any(ring_inside(x,y,h)for h in p[1:])for p in geo['coordinates'])
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--near-meters',type=int,default=300);a=ap.parse_args()
 data=json.loads((OUT/'workbook_manifest.json').read_text(encoding='utf-8'));cache=json.loads((OUT/'cache.json').read_text(encoding='utf-8'));boundary=json.loads((ROOT/'RuoYi-Vue3/src/assets/geo/dachang-boundary.json').read_text(encoding='utf-8'))
 sp=importlib.util.spec_from_file_location('g',ROOT/'SQL/geocoding/geocode_places.py');g=importlib.util.module_from_spec(sp);sp.loader.exec_module(g)
 sp=importlib.util.spec_from_file_location('c',ROOT/'tools/place-import/convert_lists.py');m=importlib.util.module_from_spec(sp);sp.loader.exec_module(m)
 selected=[];details=[];tasks=[]
 for row in data['rows']:
  query_names=[row['name'],'大厂 '+norm(row['name'])];candidates=[]
  for q in query_names:
   params=dict(keywords=q,city='南京市',citylimit='true',offset='5',page='1',extensions='base',output='JSON');fp=hashlib.sha256(json.dumps(['poi',params],ensure_ascii=False,sort_keys=True).encode()).hexdigest();answer=cache.get(fp)
   if answer:
    for c in g.candidates_from(answer,'poi',dict(record_key=row['source_key'],name=row['name'])):
     if not any(c['poi_id']==p['poi_id']and c['longitude']==p['longitude']for p in candidates):candidates.append(c)
  local=[c for c in candidates if within(c,boundary['geometry'])]
  for c in local:c['similarity']=round(score(row['name'],c['name']),3)
  plausible=[c for c in local if c['similarity']>=.65];chosen=None;diameter=0
  if plausible:
   best=max(c['similarity']for c in plausible);pool=[c for c in plausible if c['similarity']>=max(.65,best-.15)]
   diameter=max((dist(c,p)for c in pool for p in pool),default=0)
   if diameter<=a.near_meters:
    chosen=max(pool,key=lambda c:c['similarity']-.12*bool(re.search('停车场|出入口|交通车站|修理厂|厕所|餐厅',c['name'])))
    reason=f'大厂街道边界内模糊名称匹配；{len(pool)}个合理候选最大间距{diameter:.0f}米；选主体优先代表点，名称相似度{chosen['similarity']:.0%}，待核验'
   else:reason=f'街道内存在多个合理位置，最大间距{diameter:.0f}米，超过{a.near_meters}米，不能当作相近地址合并'
  elif local:reason='街道内有搜索结果，但名称关联不足，可能是无关店铺或机构'
  elif candidates:reason='搜索结果均在项目已确认的大厂街道边界之外'
  else:reason='高德未检索到有效位置，需补完整名称或门牌地址'
  selected.append(dict(excel_row=row['excel_row'],name=row['name'],selected=chosen,reason=reason,candidate_count=len(candidates),existing=bool(row['existing_longitude']or row['existing_latitude'])))
  details.append(dict(row=row,candidates=candidates,local_count=len(local),reason=reason,selected_id=chosen['candidate_id']if chosen else''))
  if not chosen:
   q=query_names[1];params=dict(keywords=q,city='南京市',citylimit='true',offset='5',page='1',extensions='base',output='JSON');fp=hashlib.sha256(json.dumps(['poi',params],ensure_ascii=False,sort_keys=True).encode()).hexdigest()
   if fp not in cache:tasks.append(dict(kind='poi',params=params,fingerprint=fp,name=row['name'],record_key=row['source_key']))
 _,cells=m.read_book(Path(data['input']));payload=dict(scope='大厂街道（项目已确认GCJ02边界）',input=data['input'],sha256=data['sha256'],sheet=data['sheet'],output=str(OUT/'监管对象_大厂街道模糊定位.xlsx'),rows=selected,original_values=[[cells.get(f'{col}{i}','')for col in 'ABCDEFGHIJKLMNOPQRSTUVWX']for i in range(1,502)])
 assert hashlib.sha256(Path(data['input']).read_bytes()).hexdigest()==data['sha256'],'源Excel已变化'
 (OUT/'fuzzy_writeback.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8');(OUT/'fuzzy_results.json').write_text(json.dumps(details,ensure_ascii=False,indent=2),encoding='utf-8');(OUT/'fuzzy_tasks.json').write_text(json.dumps(tasks,ensure_ascii=False),encoding='utf-8')
 with (OUT/'大厂街道未定位原因.csv').open('w',encoding='utf-8-sig',newline='')as f:
  w=csv.writer(f);w.writerow(['原表行号','对象名称','未定位原因','高德返回名称与地址'])
  for x,d in zip(selected,details):
   if not x['selected']:w.writerow([x['excel_row'],x['name'],x['reason'],'；'.join(c['name']+'：'+c['matched_address']for c in d['candidates'])])
 print(json.dumps(dict(total=len(selected),selected=sum(bool(x['selected'])for x in selected),pending_queries=len(tasks),unresolved_reasons=dict(collections.Counter(x['reason'].split('，')[0]for x in selected if not x['selected']))),ensure_ascii=False))
if __name__=='__main__':main()



