import datetime,gzip,hashlib,json
from db_support import connect,OUT,save

db=connect()
try:
    with db.cursor() as c:
        c.execute('START TRANSACTION WITH CONSISTENT SNAPSHOT')
        c.execute('SHOW TABLES');tables=[next(iter(r.values()))for r in c.fetchall()]
        schemas={}
        for table in tables:
            c.execute('SHOW CREATE TABLE `'+table+'`');schemas[table]=c.fetchone()['Create Table']
        c.execute("SELECT DISTINCT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME LIKE '%place%id%'")
        targets=sorted({r['TABLE_NAME']for r in c.fetchall()}|{'fire_place_contact','fire_place_import_row'})
        snapshot={'database':'dachang','schemas':schemas,'tables':{}}
        for table in targets:
            c.execute('SELECT * FROM `'+table+'`');snapshot['tables'][table]=c.fetchall()
        tag=datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f');path=OUT/('backup-before-sync-'+tag+'.json.gz')
        with gzip.open(path,'wt',encoding='utf-8')as f:json.dump(snapshot,f,ensure_ascii=False,default=str)
        with gzip.open(path,'rt',encoding='utf-8')as f:check=json.load(f)
        assert all(len(check['tables'][t])==len(rows)for t,rows in snapshot['tables'].items())
        metadata=dict(path=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),counts={t:len(rows)for t,rows in snapshot['tables'].items()},schemas=schemas)
        save('backup_manifest.json',metadata)
        print(json.dumps({k:v for k,v in metadata.items()if k!='schemas'},ensure_ascii=False))
    db.rollback()
finally:db.close()
