"""Convert the two reviewed multi-block lists to the single-sheet template; never access DB."""
import argparse,collections,csv,hashlib,json,os,re,subprocess,sys,zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
NS={'m':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
def clean(v): return re.sub(r'\s+',' ',str(v or '')).strip()
def key(v): return re.sub(r'\s+','',v)
def read_book(path):
 with zipfile.ZipFile(path) as z:
  strings=[''.join(x.itertext()) for x in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('m:si',NS)] if 'xl/sharedStrings.xml' in z.namelist() else []
  book=ET.fromstring(z.read('xl/workbook.xml'));sheet=book.find('m:sheets/m:sheet',NS)
  rels=ET.fromstring(z.read('xl/_rels/workbook.xml.rels'));rid=sheet.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id');target=next(x.get('Target')for x in rels if x.get('Id')==rid);target=target.lstrip('/') if target.startswith('/') else 'xl/'+target
  doc=ET.fromstring(z.read(target));cells={}
  for c in doc.findall('.//m:sheetData/m:row/m:c',NS):
   v=c.find('m:v',NS);value=v.text if v is not None else ''.join(c.find('m:is',NS).itertext()) if c.find('m:is',NS)is not None else ''
   if c.get('t')=='s':value=strings[int(value)]
   if c.find('m:f',NS)is not None: raise ValueError('来源包含公式，需人工确认：'+c.get('r'))
   cells[c.get('r')]=clean(value)
  # Expand only actual vertical merged ranges. Never carry categories through unmerged blanks.
  for m in doc.findall('m:mergeCells/m:mergeCell',NS):
   a,b=m.get('ref').split(':');ma=re.fullmatch(r'([A-Z]+)(\d+)',a);mb=re.fullmatch(r'([A-Z]+)(\d+)',b)
   if ma.group(1)==mb.group(1):
    for r in range(int(ma.group(2)),int(mb.group(2))+1):cells[f'{ma.group(1)}{r}']=cells.get(a,'')
  return sheet.get('name'),cells

def convert(docs):
 rows=[];excluded=[];sources=[]
 for filename,blocks in [('企业重点监管对象.xlsx',['ABCDE','FGHIJ','KLMNO','PQRST']),('消防重点监管对象.xlsx',['ABCDE','FGHIJ','KLMNO'])]:
  p=docs/filename;sheet,c=read_book(p);sha=hashlib.sha256(p.read_bytes()).hexdigest();count=0
  for block in blocks:
   mode=None
   for r in range(2,1000):
    vals=[c.get(f'{col}{r}','')for col in block];serial,second,third,person,phone=vals
    if serial=='序号':
     mode='housing' if second=='小区名称' else 'liaison' if second=='网格' else 'unit'
     if second not in ('小区名称','网格','类别'):raise ValueError(f'未知表头：{filename}/{block}/{r}')
     continue
    if not any(vals):continue
    if not re.fullmatch(r'\d+',serial):raise ValueError(f'未识别的数据行：{filename}/{block}/{r}: {vals}')
    if mode is None:raise ValueError('数据行缺少表头')
    count+=1
    origin={'file':filename,'sheet':sheet,'row':r,'block':block,'serial':serial,'raw':vals,'file_sha256':sha}
    origin['source_key']=hashlib.sha256(f'{sha}|{sheet}|{r}|{block}'.encode()).hexdigest()
    if mode=='liaison' or second=='各部门联络员':excluded.append({**origin,'reason':'社区网格联络员' if mode=='liaison' else '部门联络员'});continue
    if mode=='housing':name,company,category,typ=second,third,'','住宅小区'
    else:
     name,company,category=third,'',second
     typ='商业综合体' if '商业综合体' in category else '养老机构' if '养老机构' in category else '工贸企业' if '工贸企业' in category else '待确认'
    if not name:raise ValueError('场所名称为空：'+str(origin))
    notes=[f'来源：{sheet}!{block[0]}{r}:{block[-1]}{r}']
    if typ in ('住宅小区','待确认'):notes.append('对象类型需与现有系统字典核对')
    if not person or not phone:notes.append('负责人或联系电话缺失')
    if phone and not re.fullmatch(r'1\d{10}|0\d{2,3}[-－]?\d{7,8}(?:[-转]\d+)?',phone):notes.append('联系电话格式待核对，保留原文')
    row=[serial,category,name,person,phone,company,'','','','',typ,'','','','','','','','',filename,'；'.join(notes)]
    rows.append({'values':row,'origin':origin})
  sources.append({'file':filename,'sha256':sha,'source_records':count})
 historical=json.loads((ROOT/'SQL/geocoding/places_input.json').read_text(encoding='utf-8'))
 old=collections.defaultdict(list)
 for x in historical:old[key(x['name'])].append(x['source_key'])
 groups=collections.Counter(key(x['values'][2])for x in rows)
 for x in rows:
  name=key(x['values'][2]);notes=[]
  if name in old:notes.append('与历史导入清单同名，待核对已有对象，不自动合并');x['historical_candidates']=old[name]
  if groups[name]>1:notes.append('本批次同名记录，保留待核对')
  if notes:x['values'][20]+='；'+'；'.join(notes)
 assert sum(s['source_records']for s in sources)==len(rows)+len(excluded)
 return {'sources':sources,'records':rows,'excluded':excluded,'summary':{'places':len(rows),'excluded_liaisons':len(excluded),'types':dict(collections.Counter(x['values'][10]for x in rows)),'historical_name_matches':sum('historical_candidates'in x for x in rows),'phone_review':sum('电话格式待核对'in x['values'][20]for x in rows)}}

def main():
 ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--docs',type=Path,default=ROOT/'docs');ap.add_argument('--output-dir',type=Path,default=ROOT/'outputs/regulatory-filled');args=ap.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
 data=convert(args.docs);data['template']=str(ROOT/'outputs/regulatory-template/监管对象标准导入模板_单页版_V2.xlsx');data['output_dir']=str(args.output_dir.resolve());manifest=args.output_dir/'转换核对报告.json';manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
 with (args.output_dir/'非场所联络员.csv').open('w',encoding='utf-8-sig',newline='') as f:
  writer=csv.writer(f);writer.writerow(['来源文件','工作表','原行号','列块','序号','类别或网格','单位或社区','负责人','联系电话','分离原因'])
  for x in data['excluded']:writer.writerow([x['file'],x['sheet'],x['row'],x['block'],*x['raw'],x['reason']])
 # The bundled authoring dependency is used through a local junction, never repository npm dependencies.
 dep=Path(os.environ.get('FIRE_SPREADSHEET_RUNTIME',str(Path.home()/'.cache/codex-runtimes/codex-primary-runtime/dependencies')))
 junction=Path(__file__).parent/'node_modules'
 if not junction.exists():
  subprocess.run(['powershell','-NoProfile','-Command',f"New-Item -ItemType Junction -Path '{junction}' -Target '{dep/'node/node_modules'}' | Out-Null"],check=True)
 subprocess.run([str(dep/'node/bin/node.exe'),str(Path(__file__).with_name('fill-template.mjs')),str(manifest)],check=True)
 print(json.dumps(data['summary'],ensure_ascii=False))
if __name__=='__main__':main()

