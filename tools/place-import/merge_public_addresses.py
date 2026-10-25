"""Merge reviewed public-address evidence and AMap candidates into a preserved workbook copy."""
import collections,csv,hashlib,importlib.util,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'outputs/regulatory-geocoded'
def mod(n,p):
 s=importlib.util.spec_from_file_location(n,p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
f=mod('f',ROOT/'tools/place-import/fuzzy_dachang.py');g=mod('g',ROOT/'SQL/geocoding/geocode_places.py');c=mod('c',ROOT/'tools/place-import/convert_lists.py')
def cached(cache,kind,params):return cache.get(hashlib.sha256(json.dumps([kind,params],ensure_ascii=False,sort_keys=True).encode()).hexdigest(),{})
def main():
 cache=json.loads((OUT/'cache.json').read_text(encoding='utf-8'));sources=json.loads((OUT/'reviewed_public_addresses.json').read_text(encoding='utf-8'));searches=json.loads((OUT/'public_address_search.json').read_text(encoding='utf-8'));prior=json.loads((OUT/'fuzzy_writeback.json').read_text(encoding='utf-8'));boundary=json.loads((ROOT/'RuoYi-Vue3/src/assets/geo/dachang-boundary.json').read_text(encoding='utf-8'))['geometry'];byname={r['name']:r for r in sources};byrow={r['row']:r for r in searches};base=Path(prior['output']);sheet,cells=c.read_book(base);metadata=[];added=0
 for r in prior['rows']:
  if r['selected']:continue
  i=r['excel_row'];source=byname.get(r['name']);chosen=None;allc=[];reason='本轮公开检索未核实到与该对象相对应的完整地址；'+r['reason'];outside=None
  if source:
   reason=source['note']or'公开地址待确认实际经营场所'
   if source['query']:
    queryrow=dict(name=source['full_name'],record_key='public:'+str(i))
    geo=cached(cache,'geocode',dict(address=source['address'],city='南京市',output='JSON'));allc=g.candidates_from(geo,'geocode',queryrow)
    precise=[p for p in allc if p['level']in g.PRECISE];inside=[p for p in precise if f.within(p,boundary)];pool=inside
    if pool and max((f.dist(p,q)for p in pool for q in pool),default=0)<=300:
     priority={'单元号':0,'门址':1,'门牌号':2,'住宅区':3,'兴趣点':4};chosen=min(pool,key=lambda x:priority.get(x['level'],99));reason='公开地址经高德精细解析，多个候选不超过300米，位于大厂边界内；'+source['address_type']+'，仍需核对实际经营场所'
    if not chosen:
     poi=cached(cache,'poi',dict(keywords=source['full_name'],city='南京市',citylimit='true',offset='5',page='1',extensions='base',output='JSON'));pc=g.candidates_from(poi,'poi',queryrow);allc+=pc;pool=[p for p in pc if f.within(p,boundary)and f.score(source['full_name'],p['name'])>=.8]
     exact=[p for p in pool if f.norm(source['full_name'])==f.norm(p['name'])]
     if exact:pool=exact
     if pool and max((f.dist(p,q)for p in pool for q in pool),default=0)<=300:
      chosen=max(pool,key=lambda p:f.score(source['full_name'],p['name']));reason='按公开资料补全企业全称后，高德同名近距POI位于大厂边界内；地址仍需核验'
    if not chosen:
     if precise and not inside:reason='公开地址经高德解析位于大厂街道边界外，未写入场所经纬度；'+source['address_type']
     elif inside:reason='公开地址在街道内存在多个相距超过300米的位置，仍有歧义'
     else:reason='已查到公开地址，但高德返回精度不足或名称/地址仍有歧义，未写坐标'
    outside=next(iter(precise),None)
   else:reason='已找到地址线索但暂不定位：'+source['note']
  else:
   q=byrow.get(i,{})
   if q.get('sources'):reason='公开检索出现相关页面，但无法核实页面地址属于该企业，未采纳；'+r['reason']
  if chosen:added+=1
  candidate=chosen or outside
  source_urls=source['source_url']if source else '\n'.join(x['url']for x in byrow.get(i,{}).get('sources',[]))
  metadata.append(dict(excel_row=i,name=r['name'],source=source,chosen=chosen,candidates=allc,reason=reason,values=[source['full_name']if source else'',source['address']if source else'',source['address_type']if source else'',source_urls,'2026-10-25',candidate['longitude']if candidate else'',candidate['latitude']if candidate else'',candidate['matched_address']if candidate else'',candidate['level']or candidate['method']if candidate else'',('大厂街道范围内'if f.within(candidate,boundary)else'大厂街道范围外')if candidate else'',reason]))
 cols=[chr(65+i)for i in range(26)]+['A'+chr(65+i)for i in range(5)]
 payload=dict(input=str(base),sha256=hashlib.sha256(base.read_bytes()).hexdigest(),sheet=sheet,output=str(OUT/'监管对象_公开地址补查定位.xlsx'),original_values=[[cells.get(f'{col}{i}','')for col in cols]for i in range(1,502)],rows=metadata,summary=dict(total=325,previous_coordinates=173,additional_coordinates=added,coordinates_total=173+added,remaining=152-added,searched_records=152,reviewed_address_leads=len(sources),records_with_address=sum(bool(x['source'])for x in metadata)))
 (OUT/'public_writeback.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8')
 with (OUT/'公开地址补查结果.csv').open('w',encoding='utf-8-sig',newline='')as file:
  w=csv.writer(file);w.writerow(['Excel行号','原对象名称','查得全称','公开地址','地址性质','来源链接','查证日期','地址候选经度','地址候选纬度','高德地址','精度','范围','处理结论'])
  for r in metadata:w.writerow([r['excel_row'],r['name'],*r['values']])
 print(json.dumps(payload['summary'],ensure_ascii=False))
if __name__=='__main__':main()

