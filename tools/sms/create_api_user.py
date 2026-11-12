"""Create a dedicated user through existing RuoYi admin APIs. No database passwords needed."""
import argparse, datetime, json, os, re, secrets, string, sys
from pathlib import Path
import urllib.error, urllib.parse, urllib.request

ROOT = Path(__file__).resolve().parents[2]
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl): return None

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--username',required=True)
    p.add_argument('--dept-id',required=True,type=int)
    p.add_argument('--nickname',default='短信服务账号')
    args=p.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_]{2,20}',args.username) or args.dept_id<1:
        p.error('Use 2-20 ASCII letters/digits/underscore and a valid department ID')
    base=os.environ['RUOYI_BASE_URL'].rstrip('/')
    token=os.environ['RUOYI_ADMIN_TOKEN']
    if not base.startswith(('http://','https://')): p.error('Invalid API base URL')
    http=urllib.request.build_opener(NoRedirect())
    def request(method,path,body=None):
        headers={'Authorization':'Bearer '+token,'Accept':'application/json'}
        data=None
        if body is not None:
            headers['Content-Type']='application/json; charset=UTF-8';data=json.dumps(body,ensure_ascii=False).encode('utf-8')
        with http.open(urllib.request.Request(base+path,data=data,headers=headers,method=method),timeout=30) as r:
            result=json.loads(r.read().decode('utf-8'))
        if result.get('code')!=200: raise RuntimeError('Admin API refused request (code='+str(result.get('code'))+'); check admin permissions, captcha/session and deployment')
        return result
    existing=request('GET','/system/user/list?'+urllib.parse.urlencode({'userName':args.username,'pageNum':1,'pageSize':1000}))
    if any(u.get('userName')==args.username for u in existing.get('rows',[])):
        raise RuntimeError('Username already exists; no account was modified')
    roles=request('GET','/system/role/list?'+urllib.parse.urlencode({'roleKey':'sms_api_client','pageNum':1,'pageSize':1000}))
    matches=[r for r in roles.get('rows',[]) if r.get('roleKey')=='sms_api_client' and r.get('status')=='0']
    if len(matches)!=1: raise RuntimeError('Exactly one enabled sms_api_client role is required; apply SQL/fire_sms_open.sql first')
    # 20 characters, with every category; within the existing RuoYi password length limit.
    alphabet=string.ascii_letters+string.digits+'!@#%_-'
    while True:
        password=''.join(secrets.choice(alphabet) for _ in range(20))
        if all(any(c in group for c in password) for group in [string.ascii_lowercase,string.ascii_uppercase,string.digits,'!@#%_-']): break
    private=ROOT/'work/sms-open-private';private.mkdir(parents=True,exist_ok=True)
    file=private/(args.username+'-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'-'+secrets.token_hex(3)+'.json')
    credentials=dict(baseUrl=base,username=args.username,password=password,status='prepared',role='sms_api_client',deptId=args.dept_id)
    with file.open('x',encoding='utf-8') as f: json.dump(credentials,f,ensure_ascii=False,indent=2)
    try:
        request('POST','/system/user',dict(userName=args.username,nickName=args.nickname,deptId=args.dept_id,password=password,status='0',sex='2',roleIds=[matches[0]['roleId']],postIds=[],remark='短信专用服务账号'))
    except Exception:
        print('Creation not confirmed; credentials retained at '+str(file)+'. Check user management before retrying.',file=sys.stderr)
        raise
    credentials['status']='created';file.write_text(json.dumps(credentials,ensure_ascii=False,indent=2),'utf-8')
    print('Created dedicated SMS service user. Private credentials: '+str(file))
    print('Do not include this file in the public caller bundle.')

if __name__=='__main__':
    try: main()
    except (RuntimeError,OSError,KeyError,ValueError) as e:
        # Never print admin tokens or generated passwords.
        print(type(e).__name__+': '+str(e),file=sys.stderr)
        sys.exit(1)
