"""Run cached supplementary Dachang name searches through the existing JS worker."""
import argparse,http.server,threading,subprocess,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];out=ROOT/'outputs/regulatory-geocoded'
ap=argparse.ArgumentParser();ap.add_argument('--tasks',default='fuzzy_tasks.json');ap.add_argument('--report',default='fuzzy_run.json');args=ap.parse_args()
class Handler(http.server.BaseHTTPRequestHandler):
 def do_GET(self):
  self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.end_headers();self.wfile.write((ROOT/'SQL/geocoding/geocoding.html').read_bytes())
 def log_message(self,*a):pass
s=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=s.serve_forever,daemon=True).start()
try:
 tasks=json.loads((out/args.tasks).read_text(encoding='utf-8'));cfg=dict(tasks=tasks,cache=str(out/'cache.json'),report=str(out/args.report),url=f'http://127.0.0.1:{s.server_port}/',maxSeconds=420)
 subprocess.run([str(Path.home()/'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'),str(ROOT/'SQL/geocoding/js_api_worker.cjs')],input=json.dumps(cfg),text=True,encoding='utf-8',check=True,timeout=440)
finally:s.shutdown();s.server_close()

