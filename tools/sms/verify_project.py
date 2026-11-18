"""Isolated MySQL + real MyBatis/DataScope + loopback SMS platform. Never reads live SMS keys."""
from pathlib import Path
import json, os, re, socket, subprocess, tempfile, time

ROOT = Path(__file__).resolve().parents[2]
BIN = Path(os.environ.get('MYSQL_BIN', r'C:\Program Files\MySQL\MySQL Server 8.0\bin'))
work = Path(tempfile.mkdtemp(prefix='fire-sms-'))
flags = getattr(subprocess, 'CREATE_NO_WINDOW', 0)

def run(args, **kw):
    result = subprocess.run([str(x) for x in args], creationflags=flags, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kw)
    if result.returncode:
        raise RuntimeError((result.stdout + result.stderr).decode('utf-8', errors='replace')[-6000:])
    return result.stdout.decode('utf-8', errors='replace')

run([BIN/'mysqld.exe', '--no-defaults', '--initialize-insecure', '--datadir='+str(work/'data')], timeout=60)
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0)); port = sock.getsockname()[1]
log = (work/'mysql.log').open('wb')
server = subprocess.Popen([str(BIN/'mysqld.exe'), '--no-defaults', '--datadir='+str(work/'data'), '--port='+str(port), '--bind-address=127.0.0.1', '--mysqlx=OFF', '--skip-log-bin', '--console'], stdout=log, stderr=log, creationflags=flags)
client = [BIN/'mysql.exe', '--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port='+str(port), '--user=root', '--default-character-set=utf8mb4', '--batch', '--skip-column-names']
def sql(query):
    return run(client, input=query.encode('utf-8'), timeout=20)

try:
    for _ in range(100):
        try: sql('SELECT 1;'); break
        except RuntimeError: time.sleep(.2)
    else: raise RuntimeError('Isolated MySQL startup timeout')
    framework = (ROOT/'SQL/ry_20260417.sql').read_text('utf-8')
    sql('CREATE DATABASE dachang CHARACTER SET utf8mb4;USE dachang;' + re.search(r'create table sys_menu\s*\([\s\S]*?;', framework, re.I).group(0))
    places = (ROOT/'SQL/fire_place_init.sql').read_text('utf-8')
    for name in ['fire_place', 'fire_place_contact']:
        sql('USE dachang;' + re.search(r'CREATE TABLE IF NOT EXISTS `'+name+r'`[\s\S]*?;', places).group(0))
    sql((ROOT/'SQL/fire_inspection.sql').read_text('utf-8'))
    schema = (ROOT/'SQL/fire_sms.sql').read_text('utf-8'); sql(schema); sql(schema)
    build = run([os.environ.get('MAVEN_CMD', r'D:\env\apache-maven-3.9.9\bin\mvn.cmd'), '-o', '-X', '-pl', 'ruoyi-admin', '-am', '-DskipTests', 'compile'], cwd=ROOT/'RuoYi-Vue-master', timeout=120)
    paths = re.findall(r'compilePath = \[([^\r\n]+)\]', build)
    if not paths: raise RuntimeError('No compiler dependency path')
    cp = os.pathsep.join(paths[-1].split(', ')); classes = work/'classes'; classes.mkdir()
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, ROOT/'tools/sms/SmsIntegrationCheck.java'], timeout=30)
    result = run(['java', '-Dfile.encoding=UTF-8', '-cp', str(classes)+os.pathsep+cp, 'SmsIntegrationCheck', port, ROOT], timeout=90)
    print(result)
    output = ROOT/'docs/verification/sms-integration.json'; output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(dict(isolatedMySQL=True, loopbackSmsPlatform=True, schemaRepeatSafe=True, businessDatabaseAccessed=False, liveSmsSent=False, result=result.strip()), ensure_ascii=False, indent=2), 'utf-8')
finally:
    try: run([BIN/'mysqladmin.exe', '--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port='+str(port), '--user=root', 'shutdown'], timeout=10)
    finally:
        try: server.wait(timeout=10)
        except subprocess.TimeoutExpired: server.terminate(); server.wait(timeout=5)
        log.close()
