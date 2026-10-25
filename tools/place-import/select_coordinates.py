"""Select only unambiguous named local POIs; retain all raw API candidates."""
import argparse,hashlib,importlib.util,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--output-dir',type=Path,default=ROOT/'outputs/regulatory-geocoded');a=ap.parse_args();out=a.output_dir
 sp=importlib.util.spec_from_file_location('geo',ROOT/'SQL/geocoding/geocode_places.py');g=importlib.util.module_from_spec(sp);sp.loader.exec_module(g)
 results=json.loads((out/'results.json').read_text(encoding='utf-8'));cache=json.loads((out/'cache.json').read_text(encoding='utf-8'));manifest=json.loads((out/'workbook_manifest.json').read_text(encoding='utf-8'))
 if hashlib.sha256(Path(manifest['input']).read_bytes()).hexdigest()!=manifest['sha256']:raise ValueError('源Excel运行期间发生变化，停止写回，请重跑查询输入')
 for r in results:
  # No fuzzy/first-hit match. A single exact normalized name may be a candidate,
  # with Nanjing city-limited request provenance even if JS omitted official adcode.
  if r['selected_id']:continue
  p={'keywords':r['row']['name'],'city':'南京市','citylimit':'true','offset':'5','page':'1','extensions':'base','output':'JSON'}
  fp=hashlib.sha256(json.dumps(['poi',p],ensure_ascii=False,sort_keys=True).encode()).hexdigest();response=cache.get(fp,{})
  same=[c for c in r['candidates'] if c['method']=='amap_poi' and g.normalize_name(c['name'])==g.normalize_name(r['row']['name']) and (not c['adcode'] or c['adcode'].startswith('3201'))]
  if len(same)==1 and int(response.get('count',999))<=5:
   c=same[0];r['selected_id']=c['candidate_id'];r['reason']='南京市限定搜索中唯一精确同名POI；地址缺失或尚未交叉核验，坐标待地图核验'
  elif not r['candidates']:r['reason']='高德未返回有效位置，或本轮请求未成功；需补充完整名称和地址'
 selected=[]
 for r,m in zip(results,manifest['rows']):
  assert r['row']['source_key']==m['source_key']
  c=next((x for x in r['candidates']if x['candidate_id']==r['selected_id']),None)
  selected.append(dict(excel_row=m['excel_row'],name=m['name'],selected=c,reason=r['reason'],candidate_count=len(r['candidates']),existing=bool(m['existing_longitude']or m['existing_latitude'])))
 payload=dict(input=manifest['input'],sha256=manifest['sha256'],sheet=manifest['sheet'],output=str(out.resolve()/'监管对象_含高德坐标_待核验.xlsx'),rows=selected,summary=dict(total=len(selected),selected=sum(bool(x['selected'])for x in selected),unresolved=sum(not x['selected']for x in selected)))
 spec=importlib.util.spec_from_file_location('convert',ROOT/'tools/place-import/convert_lists.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
 _,cells=mod.read_book(Path(manifest['input']))
 payload['original_values']=[[cells.get(f'{col}{i}','')for col in 'ABCDEFGHIJKLMNOPQRSTUVWX']for i in range(1,502)]
 (out/'writeback.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8');(out/'review_results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(payload['summary']))
if __name__=='__main__':main()


