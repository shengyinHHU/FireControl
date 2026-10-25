"""Bounded JS API run; output candidates and guarded SQL, never connect to a database."""
import argparse,hashlib,http.server,importlib.util,json,os,subprocess,sys,threading,time
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('geo',HERE/'geocode_places.py');g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
NODE=Path.home()/'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
class Missing(Exception):
 def __init__(self,kind,params):self.kind=kind;self.params=params

def fingerprint(kind,params):
 return hashlib.sha256(json.dumps([kind,params],ensure_ascii=False,sort_keys=True).encode()).hexdigest()

def main():
 ap=argparse.ArgumentParser(description=__doc__)
 ap.add_argument('--max-seconds',type=int,default=240)
 ap.add_argument('--html',type=Path,default=HERE/'geocoding.html')
 ap.add_argument('--input',type=Path,default=HERE/'places_input.json')
 ap.add_argument('--offline',action='store_true')
 ap.add_argument('--output',type=Path,default=HERE/'js_output')
 args=ap.parse_args()
 if not 10<=args.max_seconds<=600:raise ValueError('max-seconds must be 10..600')
 if not args.offline and not args.html.is_file():raise ValueError('Official example HTML missing: '+str(args.html))
 out=args.output;out.mkdir(parents=True,exist_ok=True)
 rows=g.load_places(args.input);deadline=time.monotonic()+args.max_seconds
 cachepath=out/'cache.json';cache=json.loads(cachepath.read_text('utf-8')) if cachepath.exists() else {}
 class Handler(http.server.BaseHTTPRequestHandler):
  def do_GET(self):
   if self.path!='/geocoding.html':self.send_error(404);return
   body=args.html.read_bytes();self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.end_headers();self.wfile.write(body)
  def log_message(self,*a):pass
 server=None
 if not args.offline:
  server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler)
  threading.Thread(target=server.serve_forever,daemon=True).start()
 class Replay:
  def __init__(self,skip_geo=False):self.skip_geo=skip_geo
  def get(self,kind,params):
   f=fingerprint(kind,params)
   if f in cache:return cache[f]
   if kind=='geocode' and self.skip_geo:return {'status':'1','geocodes':[]}
   raise Missing(kind,params)
 def task(kind,params,row):return dict(kind=kind,params=params,fingerprint=fingerprint(kind,params),name=row['name'],record_key=row['record_key'])
 def execute(tasks,phase):
  nonlocal cache
  if args.offline or not tasks:return ''
  remaining=int(deadline-time.monotonic())
  if remaining<15:return '达到总时限；已有缓存保留'
  cfg=dict(tasks=tasks,cache=str(cachepath),report=str(out/(phase+'_run.json')),url='http://127.0.0.1:'+str(server.server_port)+'/geocoding.html',maxSeconds=remaining)
  child=subprocess.Popen([str(NODE),str(HERE/'js_api_worker.cjs')],stdin=subprocess.PIPE,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
  try:
   child.communicate(json.dumps(cfg).encode('utf-8'),timeout=remaining+8)
  except (subprocess.TimeoutExpired,KeyboardInterrupt):
   subprocess.run(['taskkill','/PID',str(child.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=5,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
   child.wait(timeout=5)
   print('已停止本批浏览器进程，缓存保留。',flush=True)
  if cachepath.exists():cache=json.loads(cachepath.read_text('utf-8'))
  reportpath=out/(phase+'_run.json')
  report=json.loads(reportpath.read_text('utf-8')) if reportpath.exists() else {}
  return report.get('stopped','')
 stopped=''
 try:
  tasks=[]
  for row in rows:
   address=g.query_address(row)
   if address:
    p={'address':address,'city':'南京市','output':'JSON'}
    if fingerprint('geocode',p) not in cache:tasks.append(task('geocode',p,row))
  stopped=execute(tasks,'geocode')
  if not stopped:
   tasks=[]
   for row in rows:
    try:g.resolve(row,Replay(skip_geo=True))
    except Missing as m:
     if m.kind=='poi':tasks.append(task(m.kind,m.params,row))
   stopped=execute(tasks,'poi')
 finally:
  if server:server.shutdown();server.server_close()
 results=[]
 for row in rows:
  try:r=g.resolve(row,Replay(skip_geo=True))
  except Missing:
   address=g.query_address(row);p={'address':address,'city':'南京市','output':'JSON'}
   candidates=g.candidates_from(cache.get(fingerprint('geocode',p),{}),'geocode',row)
   r=dict(row=row,query_address=address,candidates=candidates,selected_id='',reason='无精细唯一结果或补充搜索未完成，需人工核验')
  results.append(r)
 g.atomic_json(out/'results.json',results)
 g.export(results,out,{'stopped_reason':stopped,'review_mode':False,'input':str(args.input),'coordinate_status':'review','api':'AMap JS API','cache_queries':len(cache)},len(cache))
 counts={}
 for r in results:
  label=next((c['level'] or c['method'] for c in r['candidates'] if c['candidate_id']==r['selected_id']),'unresolved')
  counts[label]=counts.get(label,0)+1
 print(json.dumps({'records':len(results),'levels':counts,'stopped':stopped},ensure_ascii=False),flush=True)
if __name__=='__main__':main()
