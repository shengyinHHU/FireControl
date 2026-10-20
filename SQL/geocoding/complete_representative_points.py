"""Apply the user's explicit representative-point choice to the remaining 31 records.
Only uses existing cached API candidates; no network or database writes.
"""
import copy,csv,importlib.util,io,json
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('geo',HERE/'geocode_places.py');g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
BASE=HERE/'js_output';OUT=BASE/'full_101'
# Explicitly choose the institution/residential POI, excluding shops, parking and transit stops.
POI_CHOICES={
 '新天地四季花街':'四季花街',
 '钻石星城':'开宁钻石星城',
 '化建新一村53-55':'化建新一村',
 '扬子1村':'扬子一村',
 '扬子6村':'扬子六村',
 '扬子19村':'扬子十九村',
 '扬子14村（10-11）':'扬子十四村',
 '扬子20村':'扬子二十村',
 '扬子14村':'扬子十四村',
 '扬子21村':'扬子21村',
 '山居花园':'大厂山居花园',
 '云玥美著苑':'云玥美著',
 '暄玥府':'亚东金基·暄玥府',
 '南京众悦机械设备有限责任公司':'南京众悦机械设备有限责任公司',
 '南京驰纳包装材料有限公司':'南京驰纳包装材料有限公司',
 '南京永利购物中心有限公司':'永利购物中心',
}
def main():
 original=json.loads((BASE/'results.json').read_text('utf-8'));results=copy.deepcopy(original);additional=[]
 assert len(results)==101 and sum(bool(r['selected_id']) for r in results)==70
 for r in results:
  if r['selected_id']:continue
  name=r['row']['name']
  if name in POI_CHOICES:
   candidates=[c for c in r['candidates'] if c['method']=='amap_poi' and c['name']==POI_CHOICES[name]]
   assert len(candidates)==1,(name,len(candidates))
   chosen=candidates[0]
   rationale='按用户授权选择机构/小区本体代表点；不要求精确到具体门或楼栋'
  else:
   # Prefer one actual address point over unrelated fuzzy POI hits. Same-level ties
   # are resolved deterministically by candidate ID; coordinates are never averaged.
   candidates=[c for c in r['candidates'] if c['method']=='amap_geocode' and c['eligible']]
   assert candidates,name
   rank={'门址':0,'门牌号':1,'单元号':2,'住宅区':3,'兴趣点':4}
   chosen=min(candidates,key=lambda c:(rank.get(c['level'],99),c['candidate_id']))
   rationale='按用户授权从原始地址精细候选中选一个代表点，优先门址'
  assert g.location(str(chosen['longitude'])+','+str(chosen['latitude']))
  r['selected_id']=chosen['candidate_id'];r['reason']=rationale
  r['representative_point_authorized_by_user']=True
  if chosen['method']=='amap_poi':
   chosen['in_nanjing']=True
   chosen['region_basis']='JS PlaceSearch city=南京市,citylimit=true; user-authorized representative selection'
  if name in ('化建新一村53-55','扬子14村（10-11）'):
   r['reason']+='；采用小区代表点，未核实到指定楼栋'
  additional.append(r)
 assert len(additional)==31 and all(r['selected_id'] for r in results)
 for before,after in zip(original,results):
  assert any(c['candidate_id']==after['selected_id'] for c in before['candidates'])
  if before['selected_id']:assert before['selected_id']==after['selected_id']
 OUT.mkdir(exist_ok=True)
 g.atomic_json(OUT/'results_all_101.json',results)
 g.export(results,OUT,{'review_mode':False,'stopped_reason':'','api':'cached AMap JS API','selection_policy':'User authorizes one reasonable representative point per institution; no confirmed status assigned','existing_candidates_preserved':70,'additional_representative_selections':31},0)
 sql,count=g.generate_sql(additional);assert count==31
 (OUT/'update_coordinates_additional_31.sql').write_text(sql,encoding='utf-8')
 fields=['机构名称','原始地址','经度','纬度','选中点名称','匹配地址','匹配级别','坐标来源','选择依据','本次补充','source_key']
 b=io.StringIO(newline='');w=csv.DictWriter(b,fieldnames=fields);w.writeheader()
 for r in results:
  c=next(c for c in r['candidates'] if c['candidate_id']==r['selected_id'])
  w.writerow(dict(zip(fields,[r['row']['name'],r['row']['address'],c['longitude'],c['latitude'],c['name'],c['matched_address'],c['level'],c['method'],r['reason'],'是' if r.get('representative_point_authorized_by_user') else '否',r['row']['source_key']])))
 (OUT/'机构坐标清单_101家.csv').write_text(b.getvalue(),encoding='utf-8-sig')
 print(json.dumps({'all':len(results),'preserved':70,'additional':len(additional),'network_requests':0},ensure_ascii=False))
if __name__=='__main__':main()