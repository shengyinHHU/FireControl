"""Real HTTP/JWT/Redis/MySQL tests; isolated processes and loopback SMS only."""
from pathlib import Path
import json, os, re, socket, subprocess, tempfile, time

ROOT = Path(__file__).resolve().parents[2]
MYSQL = Path(os.environ.get('MYSQL_BIN', r'C:\Program Files\MySQL\MySQL Server 8.0\bin'))
REDIS = Path(os.environ.get('REDIS_SERVER', r'E:\software\Redis-x64-5.0.14.1\redis-server.exe'))
FLAGS = getattr(subprocess, 'CREATE_NO_WINDOW', 0)
work = Path(tempfile.mkdtemp(prefix='fire-sms-open-'))
def run(args, **kw):
    r = subprocess.run(list(map(str, args)), creationflags=FLAGS, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kw)
    if r.returncode:
        raise RuntimeError((r.stdout+r.stderr).decode('utf-8', errors='replace')[-14000:])
    return r.stdout.decode('utf-8', errors='replace')
def port():
    with socket.socket() as s:
        s.bind(('127.0.0.1', 0)); return s.getsockname()[1]
mysql_port, redis_port = port(), port()
run([MYSQL/'mysqld.exe', '--no-defaults', '--initialize-insecure', '--datadir='+str(work/'data')], timeout=60)
logs, processes = [], []
try:
    for name, args in [
        ('mysql', [MYSQL/'mysqld.exe', '--no-defaults', '--datadir='+str(work/'data'), '--port='+str(mysql_port), '--bind-address=127.0.0.1', '--mysqlx=OFF', '--skip-log-bin', '--console']),
        ('redis', [REDIS, '--bind', '127.0.0.1', '--port', redis_port, '--save', '', '--appendonly', 'no', '--dir', work])]:
        log=(work/(name+'.log')).open('wb'); logs.append(log)
        processes.append(subprocess.Popen(list(map(str,args)), stdout=log, stderr=log, creationflags=FLAGS))
    client=[MYSQL/'mysql.exe', '--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port='+str(mysql_port), '--user=root', '--default-character-set=utf8mb4', '--batch', '--skip-column-names']
    def sql(value): return run(client, input=value.encode('utf-8'), timeout=20)
    for _ in range(100):
        try: sql('SELECT 1'); break
        except RuntimeError: time.sleep(.2)
    else: raise RuntimeError('MySQL startup timeout')
    for _ in range(100):
        try:
            with socket.create_connection(('127.0.0.1', redis_port), timeout=1) as s:
                s.sendall(b'*1\r\n$4\r\nPING\r\n'); assert s.recv(64).startswith(b'+PONG')
            break
        except (OSError, AssertionError): time.sleep(.1)
    else: raise RuntimeError('Redis startup timeout')
    framework=(ROOT/'SQL/ry_20260417.sql').read_text('utf-8')
    sql('CREATE DATABASE dachang CHARACTER SET utf8mb4')
    for name in ['sys_menu','sys_role','sys_role_menu','sys_user','sys_user_role','sys_dept']:
        sql('USE dachang;'+re.search(r'create table '+name+r'\s*\([\s\S]*?;', framework, re.I).group(0))
    for name in ['fire_sms.sql','fire_sms_open.sql']:
        schema=(ROOT/'SQL'/name).read_text('utf-8'); sql(schema); sql(schema)
    build=run([os.environ.get('MAVEN_CMD',r'D:\env\apache-maven-3.9.9\bin\mvn.cmd'),'-o','-X','-pl','ruoyi-admin','-am','-DskipTests','compile'],cwd=ROOT/'RuoYi-Vue-master',timeout=120)
    cp=os.pathsep.join(re.findall(r'compilePath = \[([^\r\n]+)\]',build)[-1].split(', '))
    classes=work/'classes'; classes.mkdir()
    run(['javac','-encoding','UTF-8','-parameters','-cp',cp,'-d',classes,ROOT/'tools/sms/SmsOpenIntegrationCheck.java'],timeout=40)
    result=run(['java','-Dfile.encoding=UTF-8','-cp',str(classes)+os.pathsep+cp,'SmsOpenIntegrationCheck',mysql_port,redis_port,ROOT],timeout=120)
    summary=next(json.loads(line) for line in reversed(result.splitlines()) if line.startswith('{') and 'vendorCallsToLoopbackOnly' in line)
    print(json.dumps(summary,ensure_ascii=False))
    (ROOT/'docs/verification/sms-open-integration.json').write_text(json.dumps(dict(isolatedMySQL=True,isolatedRedis=True,realHttpSecurity=True,realPasswordAuthentication=True,productionRedisSerializer=True,schemaRepeatSafe=True,businessDatabaseAccessed=False,liveSmsSent=False,result=summary),ensure_ascii=False,indent=2),'utf-8')
finally:
    for process in processes:
        if process.poll() is None:
            process.terminate()
            try: process.wait(timeout=10)
            except subprocess.TimeoutExpired: process.kill(); process.wait()
    for log in logs: log.close()
