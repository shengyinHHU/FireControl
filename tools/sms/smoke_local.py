"""Boot the packaged application on loopback temporarily. Authenticate the local service user; never send SMS."""
from pathlib import Path
import argparse, json, os, socket, subprocess, time, urllib.error, urllib.request

ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--workspace-root',action='store_true',help='Verify IDE-style startup from the workspace root')
args=parser.parse_args()
PRIVATE=ROOT/'work/sms-open-private'
credentials=json.loads((PRIVATE/'sms_api-local-credentials.json').read_text('utf-8'))
with socket.socket()as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
base='http://127.0.0.1:'+str(port)
def request(path,method='GET',body=None,token=None):
    headers={'Accept':'application/json','User-Agent':'SMS-local-smoke'}
    data=None
    if body is not None:headers['Content-Type']='application/json';data=json.dumps(body).encode()
    if token:headers['Authorization']='Bearer '+token
    try:r=urllib.request.urlopen(urllib.request.Request(base+path,data=data,headers=headers,method=method),timeout=10)
    except urllib.error.HTTPError as error:r=error
    with r:return r.status,json.loads(r.read().decode('utf-8'))

log=(PRIVATE/'local-smoke.log').open('wb')
# Oracle's Windows javapath launcher can spawn a child JVM; use the actual executable
# so terminating the temporary server also releases the packaged JAR.
java=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Java\jdk-17'))/'bin/java.exe'
process=subprocess.Popen([str(java),'-jar',str(ROOT/'RuoYi-Vue-master/ruoyi-admin/target/ruoyi-admin.jar'),'--server.address=127.0.0.1','--server.port='+str(port),'--fire.device.enabled=false','--fire.incident119.enabled=false','--fire.video.enabled=false','--fire.video.dahua.enabled=false','--spring.quartz.auto-startup=false'],cwd=ROOT if args.workspace_root else ROOT/'RuoYi-Vue-master',stdout=log,stderr=log,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
try:
    for _ in range(150):
        if process.poll()is not None:raise RuntimeError('Application startup failed; inspect private local-smoke.log')
        try:
            if request('/fire/sms/open/requests/5b93fa88-e117-4ff6-a43c-970a517f35aa')[0]==401:break
        except (OSError,ValueError):pass
        time.sleep(.3)
    else:raise RuntimeError('Startup timeout; inspect private local-smoke.log')
    status,login=request('/fire/sms/open/token','POST',{'username':credentials['username'],'password':credentials['password']})
    if status!=200 or 'token'not in login:raise RuntimeError('Dedicated local account authentication failed; HTTP '+str(status))
    token=login['token']
    assert request('/fire/sms/open/requests/5b93fa88-e117-4ff6-a43c-970a517f35aa',token=token)[0]==404
    assert request('/fire/sms/open/send','POST',dict(requestId='5b93fa88-e117-4ff6-a43c-970a517f35aa',mobiles=[],content='invalid fixture'),token)[0]==400
    assert request('/fire/sms/list',token=token)[1]['code']==403
    assert request('/logout','POST',token=token)[1]['code']==200
    assert request('/fire/sms/open/requests/5b93fa88-e117-4ff6-a43c-970a517f35aa',token=token)[0]==401
    result=dict(packagedApplicationBooted=True,workingDirectory='workspace-root' if args.workspace_root else 'backend-root',localDatabaseAccountAuthenticated=True,authenticatedMissingRequest404=True,invalidSendRejectedBeforeDispatch=True,globalSmsListDenied=True,logoutRevokesToken=True,temporaryLoopbackServer=True,liveSmsSent=False)
    (ROOT/'docs/verification/sms-open-local-smoke.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),'utf-8')
    print(json.dumps(result))
finally:
    process.terminate()
    try:process.wait(timeout=15)
    except subprocess.TimeoutExpired:process.kill();process.wait()
    log.close()
