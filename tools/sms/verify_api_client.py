"""Check the public Python client against a controlled HTTP API, never the SMS provider."""
from pathlib import Path
import importlib.util, json, sys, threading, unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

root=Path(__file__).resolve().parents[2]
sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('sms_client',root/'docs/sms-api/sms_client.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
request_id='5b93fa88-e117-4ff6-a43c-970a517f35aa'
class Handler(BaseHTTPRequestHandler):
    calls=[]; mode='ok'; logins=0
    def log_message(self,*args): pass
    def reply(self,code,value,extra=None):
        self.send_response(code);self.send_header('Content-Type','application/json')
        for k,v in (extra or {}).items(): self.send_header(k,v)
        self.end_headers();self.wfile.write(json.dumps(value).encode())
    def do_POST(self):
        body=json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        Handler.calls.append((self.path,body,self.headers.get('Authorization')))
        if self.path.endswith('/token'):
            Handler.logins+=1;return self.reply(200,dict(code=200,token='fixture-'+str(Handler.logins)))
        if Handler.mode=='401-once':
            Handler.mode='ok';return self.reply(401,dict(code=401,msg='expired'))
        if Handler.mode=='429': return self.reply(429,dict(code=429,msg='quota'),{'Retry-After':'60'})
        if Handler.mode=='500': return self.reply(500,dict(code=500,msg='unknown'))
        if Handler.mode=='redirect': return self.reply(307,dict(code=307,msg='redirect'),{'Location':'/must-not-follow'})
        self.reply(200,dict(code=200,data=dict(requestId=body['requestId'],taskStatus='submitted')))
    def do_GET(self):
        Handler.calls.append((self.path,None,self.headers.get('Authorization')))
        self.reply(200,dict(code=200,data=dict(requestId=request_id,taskStatus='submitted')))

class TestClient(unittest.TestCase):
    def setUp(self):
        Handler.calls=[];Handler.mode='ok';Handler.logins=0
        self.client=module.SmsClient(base,'sms_api','dummy-password')
        self.body=dict(requestId=request_id,mobiles=['13800000001'],content='中文 + " &',bizId='test')
    def test_login_send_query(self):
        self.assertEqual(self.client.send(self.body)['data']['taskStatus'],'submitted')
        self.assertEqual(Handler.calls[-1][1],self.body)
        self.assertEqual(Handler.calls[-1][2],'Bearer fixture-1')
        self.client.query(request_id);self.assertEqual(Handler.logins,1)
    def test_401_once_reuses_payload(self):
        Handler.mode='401-once';self.client.send(self.body)
        sends=[x[1] for x in Handler.calls if x[0].endswith('/send')]
        self.assertEqual(sends,[self.body,self.body]);self.assertEqual(Handler.logins,2)
    def test_429_does_not_retry(self):
        Handler.mode='429'
        with self.assertRaises(module.SmsError) as e: self.client.send(self.body)
        self.assertEqual(e.exception.retry_after,'60');self.assertEqual(len(Handler.calls),2)
    def test_500_does_not_retry(self):
        Handler.mode='500'
        with self.assertRaises(module.SmsError): self.client.send(self.body)
        self.assertEqual(len(Handler.calls),2)
    def test_no_redirect_with_credentials(self):
        Handler.mode='redirect'
        with self.assertRaises(module.SmsError) as e: self.client.send(self.body)
        self.assertEqual(e.exception.status,307);self.assertEqual(len(Handler.calls),2)
    def test_query_uuid_validation(self):
        with self.assertRaises(ValueError):self.client.query('not-a-uuid')
        self.assertEqual(len(Handler.calls),0)

if __name__=='__main__':
    server=ThreadingHTTPServer(('127.0.0.1',0),Handler);base='http://127.0.0.1:'+str(server.server_port)
    thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
    try:
        suite=unittest.defaultTestLoader.loadTestsFromTestCase(TestClient)
        result=unittest.TextTestRunner(verbosity=1).run(suite)
        (root/'docs/verification/sms-api-python.json').write_text(json.dumps(dict(tests=result.testsRun,passed=result.wasSuccessful(),loopbackOnly=True,liveSmsSent=False),indent=2),'utf-8')
        if not result.wasSuccessful():raise SystemExit(1)
    finally:server.shutdown();server.server_close();thread.join()
