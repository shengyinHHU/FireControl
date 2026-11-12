"""Provision only the project's local MySQL SMS schema/account. Never contacts the SMS platform.
Requires the already installed PyYAML and PyMySQL, Java 17, and local Maven Spring crypto jars.
"""
from pathlib import Path
import argparse, datetime, json, os, re, secrets, string, subprocess, tempfile
import pymysql, yaml

ROOT=Path(__file__).resolve().parents[2]
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--username',default='sms_api')
    parser.add_argument('--dept-id',required=True,type=int)
    args=parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_]{2,20}',args.username):parser.error('Invalid username')
    master=yaml.safe_load((ROOT/'RuoYi-Vue-master/ruoyi-admin/src/main/resources/application-druid.yml').read_text('utf-8'))['spring']['datasource']['druid']['master']
    match=re.search(r'jdbc:mysql://([^/:]+)(?::(\d+))?/([^?]+)',master['url'])
    if not match or match[1] not in ('localhost','127.0.0.1','::1'):
        raise RuntimeError('This helper only permits the configured local database; use admin APIs for deployment')
    private=ROOT/'work/sms-open-private';private.mkdir(parents=True,exist_ok=True)
    credentials_file=private/(args.username+'-local-credentials.json')
    if credentials_file.exists():raise RuntimeError('Private credentials already exist; no user or password was modified')
    with pymysql.connect(host=match[1],port=int(match[2] or 3306),user=master['username'],password=str(master['password']),database=match[3],charset='utf8mb4',autocommit=True,connect_timeout=5)as db:
        with db.cursor(pymysql.cursors.DictCursor)as cursor:
            cursor.execute('SELECT user_id FROM sys_user WHERE user_name=%s',(args.username,))
            if cursor.fetchone():raise RuntimeError('Username already exists; no user or password was modified')
            cursor.execute("SELECT dept_id FROM sys_dept WHERE dept_id=%s AND status='0' AND del_flag='0'",(args.dept_id,))
            if not cursor.fetchone():raise RuntimeError('Selected department is not active')
            # Snapshot only the relevant preexisting schema and authority rows before additive migration.
            snapshot={'database':match[3],'createdAt':datetime.datetime.now().isoformat(),'tables':{},'authority':{}}
            for table in ['fire_sms_record','fire_sms_open_meta','fire_sms_open_quota']:
                cursor.execute('SHOW TABLES LIKE %s',(table,))
                if cursor.fetchone():
                    cursor.execute('SHOW CREATE TABLE `'+table+'`');snapshot['tables'][table]=cursor.fetchone()
            for title,query in [('roles',"SELECT * FROM sys_role WHERE role_key='sms_api_client'"),('menus',"SELECT * FROM sys_menu WHERE perms IN('fire:sms:open:send','fire:sms:open:query','fire:sms:list','fire:sms:send')"),('roleMenus',"SELECT rm.* FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id WHERE r.role_key='sms_api_client'")]:
                cursor.execute(query);snapshot['authority'][title]=cursor.fetchall()
            (private/'local-before-provision.json').write_text(json.dumps(snapshot,ensure_ascii=False,indent=2,default=str),'utf-8')
            for name in ['fire_sms.sql','fire_sms_open.sql']:
                script=(ROOT/'SQL'/name).read_text('utf-8')
                script=re.sub(r'^\s*--[^\n]*','',script,flags=re.M)
                for statement in script.split(';'):
                    statement=statement.strip()
                    if statement and not statement.upper().startswith('USE '):cursor.execute(statement)
            cursor.execute("SELECT role_id FROM sys_role WHERE role_key='sms_api_client' AND status='0' AND del_flag='0'")
            roles=cursor.fetchall()
            if len(roles)!=1:raise RuntimeError('Exactly one enabled sms_api_client role required')
            role_id=roles[0]['role_id']
            cursor.execute('SELECT DISTINCT m.perms FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE rm.role_id=%s',(role_id,))
            if {r['perms'] for r in cursor.fetchall()}!={'fire:sms:open:send','fire:sms:open:query'}:
                raise RuntimeError('Existing SMS role has unexpected permissions; no account was created')
            alphabet=string.ascii_letters+string.digits+'!@#%_-'
            while True:
                password=''.join(secrets.choice(alphabet)for _ in range(20))
                if all(any(c in group for c in password)for group in [string.ascii_lowercase,string.ascii_uppercase,string.digits,'!@#%_-']):break
            maven=Path.home()/'.m2/repository'
            crypto=sorted((maven/'org/springframework/security/spring-security-crypto').glob('7.*/spring-security-crypto-*.jar'))[-1]
            logging=sorted((maven/'commons-logging/commons-logging').glob('*/commons-logging-*.jar'))[-1]
            with tempfile.TemporaryDirectory(prefix='sms-hash-')as temporary:
                source=Path(temporary)/'HashSmsPassword.java'
                source.write_text('import java.io.*; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; class HashSmsPassword { public static void main(String[] args)throws Exception { var line=new BufferedReader(new InputStreamReader(System.in)).readLine(); System.out.print(new BCryptPasswordEncoder().encode(line)); }}','utf-8')
                process=subprocess.run(['java','--class-path',os.pathsep.join(map(str,[crypto,logging])),str(source)],input=password+'\n',text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0),timeout=30)
                if process.returncode or not process.stdout.startswith('$2'):raise RuntimeError('Password hashing failed; account not created')
                hashed=process.stdout.strip()
            credentials=dict(baseUrl='http://localhost:8080',username=args.username,password=password,status='prepared',database=match[3],role='sms_api_client',deptId=args.dept_id)
            with credentials_file.open('x',encoding='utf-8')as f:json.dump(credentials,f,ensure_ascii=False,indent=2)
            db.begin()
            try:
                cursor.execute("INSERT INTO sys_user(dept_id,user_name,nick_name,user_type,password,sex,status,del_flag,pwd_update_date,create_by,create_time,remark) VALUES(%s,%s,'短信服务账号','00',%s,'2','0','0',NOW(),'admin',NOW(),'短信专用服务账号，仅开放接口权限')",(args.dept_id,args.username,hashed))
                user_id=cursor.lastrowid
                cursor.execute('INSERT INTO sys_user_role(user_id,role_id)VALUES(%s,%s)',(user_id,role_id));db.commit()
            except Exception:db.rollback();raise
            credentials.update(status='created',userId=user_id,roleId=role_id)
            credentials_file.write_text(json.dumps(credentials,ensure_ascii=False,indent=2),'utf-8')
            environment=json.loads((ROOT/'docs/sms-api/FireControl-SMS.postman_environment.json').read_text('utf-8'))
            environment['name']='FireControl SMS 本机专用账号（含密码，请勿外传）'
            for value in environment['values']:
                if value['key']in ('baseUrl','username','password'):value['value']=credentials[value['key']]
            (private/'FireControl-SMS.local.postman_environment.json').write_text(json.dumps(environment,ensure_ascii=False,indent=2),'utf-8')
            print(json.dumps(dict(created=True,username=args.username,userId=user_id,roleId=role_id,privateCredentials=str(credentials_file),liveSmsSent=False),ensure_ascii=False))

if __name__=='__main__': main()
