"""Extract current worksheet names/addresses and run the existing cached AMap JS geocoder."""
import argparse,hashlib,importlib.util,json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--input',type=Path,default=ROOT/'outputs/regulatory-filled/监管对象_已整理_单页.xlsx');ap.add_argument('--output-dir',type=Path,default=ROOT/'outputs/regulatory-geocoded');ap.add_argument('--offline',action='store_true');a=ap.parse_args();a.output_dir.mkdir(parents=True,exist_ok=True)
 spec=importlib.util.spec_from_file_location('convert',Path(__file__).with_name('convert_lists.py'));c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)
 sheet,cells=c.read_book(a.input)
 for col,label in [('C','对象名称'),('L','详细地址'),('P','经度'),('Q','纬度'),('R','坐标系')]:
  if cells.get(col+'1')!=label:raise ValueError('表头不符：'+col)
 sha=hashlib.sha256(a.input.read_bytes()).hexdigest();rows=[]
 for r in range(2,10000):
  name=cells.get(f'C{r}','')
  if not name:continue
  rows.append(dict(source_key=hashlib.sha256(f'{sha}|{sheet}|{r}'.encode()).hexdigest(),name=name,address=cells.get(f'L{r}',''),excel_row=r,existing_longitude=cells.get(f'P{r}',''),existing_latitude=cells.get(f'Q{r}','')))
 payload=dict(input=str(a.input.resolve()),sha256=sha,sheet=sheet,rows=rows)
 (a.output_dir/'workbook_manifest.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8')
 query=a.output_dir/'places_input.json';query.write_text(json.dumps([{k:x[k]for k in ['source_key','name','address']}for x in rows],ensure_ascii=False,indent=2),encoding='utf-8')
 cache=a.output_dir/'cache.json';old=ROOT/'SQL/geocoding/js_output/cache.json'
 if not cache.exists()and old.exists():cache.write_bytes(old.read_bytes())
 cmd=[sys.executable,str(ROOT/'SQL/geocoding/run_js_geocoding.py'),'--input',str(query),'--output',str(a.output_dir),'--max-seconds','600']
 if a.offline:cmd.append('--offline')
 subprocess.run(cmd,check=True)
if __name__=='__main__':main()
