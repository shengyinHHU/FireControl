import base64,hashlib,json,os,re,socket,sys
from pathlib import Path
import yaml

ROOT=Path(__file__).resolve().parents[2];OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'outputs/highrise-sync-20261125'))
from db_support import connect

def encode(data):return json.dumps(data,ensure_ascii=False,sort_keys=True,default=str)
db=connect()
try:
    with db.cursor()as cur:
        cur.execute('SHOW CREATE TABLE sys_dict_data');ddl=cur.fetchone()['Create Table']
        cur.execute('SELECT * FROM sys_dict_data ORDER BY dict_code');before=cur.fetchall()
        cur.execute("SELECT * FROM fire_place WHERE place_type='highrise' ORDER BY place_id");places=cur.fetchall()
        assert len(places)==101 and all(p['status']=='0'and p['del_flag']=='0'for p in places)
        backup=dict(dict_schema=ddl,dict_rows=before,residential_hash=hashlib.sha256(encode(places).encode()).hexdigest(),residential_count=101)
        (OUT/'dictionary-backup.json').write_text(json.dumps(backup,ensure_ascii=False,indent=2,default=str),encoding='utf-8')
        db.rollback();db.begin()
        cur.execute("SELECT * FROM sys_dict_data WHERE dict_type='fire_place_type' AND dict_value='highrise' FOR UPDATE")
        target=list(cur.fetchall());assert len(target)==1 and target[0]['dict_label']in ['高层建筑','居民小区']
        cur.execute((ROOT/'SQL/fire_place_residential_label.sql').read_text(encoding='utf-8').split('-- Invalidate',1)[0])
        cur.execute('SELECT * FROM sys_dict_data ORDER BY dict_code');after=cur.fetchall()
        assert len(after)==len(before)
        for old,new in zip(before,after):
            if old['dict_code']==target[0]['dict_code']:
                assert new['dict_label']=='居民小区' and new['dict_value']=='highrise'
                assert all(new[k]==v for k,v in old.items() if k not in ['dict_label','update_by','update_time'])
            else:assert new==old
        cur.execute("SELECT * FROM fire_place WHERE place_type='highrise' ORDER BY place_id");assert encode(cur.fetchall())==encode(places)
        cur.execute('SHOW CREATE TABLE sys_dict_data');assert cur.fetchone()['Create Table']==ddl
        db.commit()
    result=dict(dictionary_label='居民小区',dictionary_value='highrise',residential_records=101,residential_data_unchanged=True,schema_unchanged=True)
except Exception:
    db.rollback();raise
finally:db.close()

config=yaml.safe_load((ROOT/'RuoYi-Vue-master/ruoyi-admin/src/main/resources/application.yml').read_text(encoding='utf-8'))
redis=config['spring']['data']['redis']
assert redis['host']in ['localhost','127.0.0.1']
def redis_command(stream,*parts):
    values=[str(p).encode()for p in parts]
    stream.write(b'*'+str(len(values)).encode()+b'\r\n'+b''.join(b'$'+str(len(v)).encode()+b'\r\n'+v+b'\r\n'for v in values));stream.flush()
    line=stream.readline();kind=line[:1];body=line[1:-2]
    if kind==b'-':raise RuntimeError('Redis rejected selected dictionary-cache operation')
    if kind==b'$':
        n=int(body)
        if n<0:return None
        payload=stream.read(n);assert stream.read(2)==b'\r\n';return payload
    return body.decode()
try:
    with socket.create_connection((redis['host'],int(redis['port'])),timeout=5)as sock:
        with sock.makefile('rwb')as stream:
            password=str(redis.get('password')or'')
            if password:redis_command(stream,'AUTH',password)
            redis_command(stream,'SELECT',redis.get('database',0))
            cached=redis_command(stream,'GET','sys_dict:fire_place_type')
            (OUT/'dictionary-cache-backup.json').write_text(json.dumps(dict(key='sys_dict:fire_place_type',value_base64=base64.b64encode(cached).decode()if cached else None)),encoding='utf-8')
            removed=redis_command(stream,'DEL','sys_dict:fire_place_type')
            assert redis_command(stream,'EXISTS','sys_dict:fire_place_type')=='0'
            result['dictionary_cache_invalidated']=True;result['cache_entries_removed']=int(removed)
except OSError:
    result['dictionary_cache_invalidated']=False
    result['cache_note']='Local Redis unavailable; backend restart reloads dictionary'
(OUT/'dictionary-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False))
