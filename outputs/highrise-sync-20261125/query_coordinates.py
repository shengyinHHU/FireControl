import hashlib,http.server,json,subprocess,threading
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2];OUT=Path(__file__).resolve().parent
numbers={2:'二',3:'三',4:'四',5:'五',7:'七',8:'八',9:'九',10:'十',11:'十一',12:'十二',15:'十五',16:'十六',17:'十七',18:'十八'}
queries=[('扬子'+str(n)+'村','扬子'+zh+'村') for n,zh in numbers.items()]
queries.extend([('新华二村（1-13栋）',q)for q in ['新华二村1幢','新华二村13幢','新华二村西区','新华二村东区']])
queries.extend((n,n)for n in ['紫景园','草芳新苑','欣乐南村','欣乐北村','晓山公寓','南钢二村','新华四村','南化九村','新华三村','新华五村','新华六村'])
queries.extend([('晓山公寓','南京大厂杨新路195号'),('草芳新苑','草芳新苑小区'),('扬子15村','扬子十五村南区')])
tasks=[]
for name,q in queries:
    params=dict(keywords=q,city='南京市',citylimit='true',offset='5',page='1',extensions='base',output='JSON')
    fp=hashlib.sha256(json.dumps(['poi',params],ensure_ascii=False,sort_keys=True).encode()).hexdigest()
    tasks.append(dict(kind='poi',params=params,fingerprint=fp,name=name,record_key=name))
params=dict(address='南京市六合区大厂街道杨新路195号',city='南京市',output='JSON')
fp=hashlib.sha256(json.dumps(['geocode',params],ensure_ascii=False,sort_keys=True).encode()).hexdigest()
tasks.append(dict(kind='geocode',params=params,fingerprint=fp,name='晓山公寓',record_key='晓山公寓'))
(OUT/'coordinate_tasks.json').write_text(json.dumps(tasks,ensure_ascii=False,indent=2),encoding='utf-8')
if not (OUT/'coordinate_cache.json').exists() or len(json.loads((OUT/'coordinate_cache.json').read_text(encoding='utf-8')))==0:
    (OUT/'coordinate_cache.json').write_bytes((ROOT/'outputs/regulatory-geocoded/cache.json').read_bytes())
class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.end_headers();self.wfile.write(b'<!doctype html><meta charset="utf-8"><title>Coordinate lookup</title>')
    def log_message(self,*a):pass
server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler)
threading.Thread(target=server.serve_forever,daemon=True).start()
try:
    cfg=dict(tasks=tasks,cache=str(OUT/'coordinate_cache.json'),report=str(OUT/'coordinate_run.json'),url=f'http://127.0.0.1:{server.server_port}/',maxSeconds=210)
    subprocess.run([str(Path.home()/'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'),str(OUT/'coordinate_worker.cjs')],input=json.dumps(cfg),text=True,encoding='utf-8',timeout=230,check=True,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
finally:server.shutdown();server.server_close()
