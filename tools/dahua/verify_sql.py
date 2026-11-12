"""Exercise migration in a temporary, loopback-only MySQL. Never connects to project DB."""
import os,re,socket,subprocess,tempfile,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BIN=Path(os.environ.get('MYSQL_BIN',r'C:\Program Files\MySQL\MySQL Server 8.0\bin'))
flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
def run(args,**kw):return subprocess.run([str(x) for x in args],creationflags=flags,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=40,**kw)
with tempfile.TemporaryDirectory(prefix='dahua-schema-') as temp:
    temp=Path(temp)
    assert temp.resolve().parent==Path(tempfile.gettempdir()).resolve()
    init=run([BIN/'mysqld.exe','--no-defaults','--initialize-insecure','--datadir='+str(temp/'data')])
    if init.returncode:raise RuntimeError(init.stderr.decode(errors='replace'))
    with socket.socket() as probe:probe.bind(('127.0.0.1',0));port=probe.getsockname()[1]
    with (temp/'mysql.log').open('wb') as log:
        server=subprocess.Popen([str(BIN/'mysqld.exe'),'--no-defaults','--datadir='+str(temp/'data'),'--port='+str(port),'--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin','--console'],stdout=log,stderr=log,creationflags=flags)
        command=[BIN/'mysql.exe','--no-defaults','--protocol=TCP','--host=127.0.0.1','--port='+str(port),'--user=root','--default-character-set=utf8mb4','--batch','--skip-column-names']
        def sql(query,ok=True):
            p=run(command,input=query.encode('utf-8'))
            if ok and p.returncode:raise RuntimeError(p.stderr.decode(errors='replace'))
            if not ok:assert p.returncode,'Expected SQL constraint rejection'
            return p.stdout.decode('utf-8').strip()
        try:
            for _ in range(75):
                if server.poll() is not None:raise RuntimeError('Temporary MySQL exited')
                if run(command,input=b'SELECT 1').returncode==0:break
                time.sleep(.2)
            else:raise RuntimeError('MySQL startup timeout')
            framework=(ROOT/'SQL/ry_20260417.sql').read_text('utf-8')
            sql('CREATE DATABASE dachang CHARACTER SET utf8mb4;USE dachang;'+re.search(r'create table sys_menu\s*\([\s\S]*?;',framework,re.I).group(0))
            catalog=(ROOT/'SQL/fire_video_catalog.sql').read_text('utf-8')
            sql('USE dachang;'+re.search(r'CREATE TABLE IF NOT EXISTS fire_video_camera\s*\([\s\S]*?;',catalog,re.I).group(0))
            migration=(ROOT/'SQL/fire_video_dahua_talk.sql').read_text('utf-8');sql(migration);sql(migration)
            assert sql("USE dachang;SELECT COUNT(*) FROM sys_menu WHERE perms='fire:video:talk-config'")=='1'
            sql("USE dachang;INSERT INTO fire_video_camera(camera_id,source_id,device_code,device_name,last_seen_at) VALUES(1,'fixture','one','fixture',NOW()),(2,'fixture','two','fixture',NOW());INSERT INTO fire_video_talk_endpoint(camera_id,host)VALUES(1,'10.0.0.1')")
            sql("USE dachang;INSERT INTO fire_video_talk_endpoint(camera_id,host)VALUES(1,'10.0.0.2')",False)
            sql("USE dachang;INSERT INTO fire_video_talk_endpoint(camera_id,host)VALUES(2,'10.0.0.1')",False)
            sql("USE dachang;INSERT INTO fire_video_talk_endpoint(camera_id,host)VALUES(3,'10.0.0.3')",False)
            print('PASS: repeat-safe SQL; permission idempotency; camera/IP uniqueness; foreign key. Production DB untouched.')
        finally:
            try:sql('SHUTDOWN')
            finally:
                try:server.wait(timeout=15)
                except subprocess.TimeoutExpired:server.terminate();server.wait(timeout=10)
