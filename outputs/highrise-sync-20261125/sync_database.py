"""Apply the reviewed Excel reconciliation in one transaction, without DDL."""
import argparse,collections,copy,csv,datetime,decimal,gzip,hashlib,json,re
from db_support import ROOT,OUT,connect,save

def schema_key(sql):return re.sub(r'AUTO_INCREMENT=\d+\s*','',sql)
def serialized(value):return json.dumps(value,ensure_ascii=False,sort_keys=True,default=str)

def verify(cur,plan,before,schemas):
    cur.execute("SELECT * FROM fire_place WHERE place_type='highrise' ORDER BY place_id")
    actual=cur.fetchall();assert len(actual)==101,'Expected exactly 101 highrise rows'
    assert all(p['status']=='0' and p['del_flag']=='0' for p in actual)
    lookup={p['place_id']:p for p in actual}
    assert {p['name']for p in actual}=={r['name']for r in plan['rows']}
    assert len({p['name']for p in actual})==101
    for r in plan['rows']:
        p=lookup[r['id']];coord=r['coordinate']
        for col,k in [('name','name'),('address','address'),('community_name','community'),('management_company','management'),('industry_category','category'),('highrise_building_count','highrise_count')]:
            assert p[col]==r[k],(r['name'],col,p[col],r[k])
        assert p['building_area_m2']==decimal.Decimal(str(r['area']))
        assert p['coordinate_system']=='GCJ02' and p['longitude'] is not None and p['latitude'] is not None
        assert abs(float(p['longitude'])-coord['lng'])<.0000001 and abs(float(p['latitude'])-coord['lat'])<.0000001
        cur.execute("SELECT contact_name,phone FROM fire_place_contact WHERE place_id=%s AND status='0' AND del_flag='0' ORDER BY contact_id",(r['id'],))
        contacts=list(cur.fetchall());expected=[dict(contact_name=r['person'],phone=r['phone'])]if r['person'] or r['phone']else[]
        assert contacts==expected,(r['name'],'contacts',contacts,expected)
        cur.execute('SELECT raw_data,place_id FROM fire_place_import_row WHERE source_key=%s',(r['source_key'],));audit=cur.fetchone()
        assert audit and audit['place_id']==r['id']
        raw=json.loads(audit['raw_data']);assert raw['original_cells']==r['raw']['original_cells']
        assert raw['fields']==r['raw']['fields']
    cur.execute("SELECT COUNT(*) AS n FROM fire_place WHERE place_type='highrise' AND status='0' AND del_flag='0' AND coordinate_system='GCJ02' AND longitude BETWEEN -180 AND 180 AND latitude BETWEEN -90 AND 90 AND NOT (longitude=0 AND latitude=0)")
    assert cur.fetchone()['n']==101,'Not all map records have valid coordinates'
    cur.execute("SELECT * FROM fire_place WHERE place_type<>'highrise' ORDER BY place_id");other=cur.fetchall()
    assert serialized(other)==serialized([p for p in before['tables']['fire_place']if p['place_type']!='highrise']),'Other categories changed'
    canonical={r['id']for r in plan['rows']};retired={r['id']for r in plan['retire']};touched=canonical|retired
    for table,pk in [('fire_place_contact','contact_id'),('fire_place_import_row','import_row_id')]:
        cur.execute('SELECT * FROM `'+table+'` ORDER BY `'+pk+'`');after_rows=cur.fetchall()
        unchanged_ids={p[pk]for p in before['tables'][table]if p['place_id']not in touched}
        unchanged=lambda rows:sorted((p for p in rows if p[pk]in unchanged_ids),key=lambda p:p[pk])
        assert serialized(unchanged(after_rows))==serialized(unchanged(before['tables'][table])),table+' unrelated rows changed'
    for table in before['tables']:
        if table in ['fire_place','fire_place_contact','fire_place_import_row']:continue
        cur.execute('SELECT * FROM `'+table+'`');assert serialized(cur.fetchall())==serialized(before['tables'][table]),table+' changed'
    cur.execute('SHOW TABLES');tables=[next(iter(p.values()))for p in cur.fetchall()]
    assert set(tables)==set(schemas),'Table list changed'
    for table in tables:
        cur.execute('SHOW CREATE TABLE `'+table+'`');assert schema_key(cur.fetchone()['Create Table'])==schema_key(schemas[table]),table+' schema changed'
    return actual

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--apply',action='store_true');args=ap.parse_args()
    plan=json.loads((OUT/'plan.json').read_text(encoding='utf-8'))
    manifest=json.loads((OUT/'backup_manifest.json').read_text(encoding='utf-8'))
    backup_path=OUT/PathName(manifest['path'])
    assert hashlib.sha256(backup_path.read_bytes()).hexdigest()==manifest['sha256']
    with gzip.open(backup_path,'rt',encoding='utf-8')as f:before=json.load(f)
    assert hashlib.sha256((ROOT/'docs'/plan['source_file']).read_bytes()).hexdigest()==plan['source_sha256'],'Source changed'
    if not args.apply:
        print(json.dumps(dict(mode='preflight',**plan['summary'],backup=str(backup_path)),ensure_ascii=False));return
    db=connect();changes=[];batch='dachang-excel-sync-20261125';actor='excel_sync_20261125'
    try:
        with db.cursor()as c:
            c.execute('SET SESSION innodb_lock_wait_timeout=10')
            db.begin()
            c.execute("SELECT * FROM fire_place WHERE place_type='highrise' ORDER BY place_id FOR UPDATE")
            original=c.fetchall();expected=[p for p in before['tables']['fire_place']if p['place_type']=='highrise']
            assert serialized(original)==serialized(expected),'Highrise records changed since backup'
            # Incident ingestion keeps running. Compare unrelated rows with the current
            # transaction snapshot, rather than an older backup taken before new alarms.
            validation_before=copy.deepcopy(before)
            for table in before['tables']:
                c.execute('SELECT * FROM `'+table+'`');validation_before['tables'][table]=list(c.fetchall())
            original_by_id={p['place_id']:p for p in original}
            retired={r['id']for r in plan['retire']}
            c.execute("SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME LIKE '%place%id%'")
            for ref in c.fetchall():
                t,col=ref['TABLE_NAME'],ref['COLUMN_NAME']
                if t in ['fire_place','fire_place_contact','fire_place_import_row']:continue
                c.execute('SELECT COUNT(*) AS n FROM `'+t+'` WHERE `'+col+'` IN ('+','.join(['%s']*len(retired))+')',tuple(sorted(retired)))
                assert c.fetchone()['n']==0,'Referenced retirement record in '+t
            for r in plan['rows']:
                coord=r['coordinate'];area=decimal.Decimal(str(r['area']))
                values=dict(name=r['name'],place_type='highrise',industry_category=r['category'],management_company=r['management'],community_name=r['community'],address=r['address'],building_area_m2=area,highrise_building_count=r['highrise_count'],longitude=decimal.Decimal(str(coord['lng'])),latitude=decimal.Decimal(str(coord['lat'])),coordinate_system='GCJ02',coordinate_source=coord['source'],location_status=coord['status'],matched_address=coord['matched_address'],amap_poi_id=coord['poi_id'],status='0',del_flag='0',remark=r['remark'],update_by=actor)
                if r['id']is None:
                    values.update(source_key=r['source_key'],create_by=actor)
                    cols=list(values)
                    c.execute('INSERT INTO fire_place ('+','.join('`'+x+'`'for x in cols)+',create_time,update_time) VALUES ('+','.join(['%s']*len(cols))+',NOW(),NOW())',[values[x]for x in cols]);r['id']=c.lastrowid
                    changes.append(dict(action='insert',id=r['id'],name=r['name']))
                else:
                    c.execute('UPDATE fire_place SET '+','.join('`'+x+'`=%s'for x in values)+',revision=revision+1,update_time=NOW() WHERE place_id=%s',[*values.values(),r['id']])
                    changes.append(dict(action='update',id=r['id'],name=r['name']))
                # Keep past contacts as inactive history; show only the Excel project manager.
                c.execute("UPDATE fire_place_contact SET status='1',del_flag='2',update_by=%s,update_time=NOW() WHERE place_id=%s",(actor,r['id']))
                if r['person']or r['phone']:
                    c.execute("INSERT INTO fire_place_contact(place_id,contact_role,contact_name,phone,sort_order,status,del_flag,create_by,create_time,update_by,update_time,remark) VALUES(%s,'import_responsible',%s,%s,0,'0','0',%s,NOW(),%s,NOW(),%s) ON DUPLICATE KEY UPDATE contact_name=VALUES(contact_name),phone=VALUES(phone),status='0',del_flag='0',update_by=VALUES(update_by),update_time=NOW(),remark=VALUES(remark)",(r['id'],r['person'],r['phone'],actor,actor,'本次Excel大厂工作表项目经理；已解析原表合并单元格'))
                raw=dict(**r['raw'],coordinate_match=coord,source_sha256=plan['source_sha256'],merged_cells_resolved=True)
                note='按用户要求同步101个住宅小区；未建新字段，其余物业基础字段完整保留于原始来源JSON'
                c.execute("INSERT INTO fire_place_import_row(source_key,batch_no,file_name,file_sha256,sheet_name,`row_number`,source_serial,raw_data,place_id,import_status,error_message,create_by,create_time) VALUES(%s,%s,%s,%s,'大厂',%s,%s,%s,%s,'imported',%s,%s,NOW()) ON DUPLICATE KEY UPDATE raw_data=VALUES(raw_data),place_id=VALUES(place_id),import_status='imported',error_message=VALUES(error_message)",(r['source_key'],batch,plan['source_file'],plan['source_sha256'],r['row'],str(r['raw']['fields']['序号']),json.dumps(raw,ensure_ascii=False),r['id'],note,actor))
            for r in plan['retire']:
                if r['target']:
                    c.execute('UPDATE fire_place_import_row SET place_id=%s WHERE place_id=%s',(r['target'],r['id']))
                else:
                    c.execute("UPDATE fire_place_import_row SET place_id=NULL,import_status='review',error_message=%s WHERE place_id=%s",('原扬子小区汇总档案已按用户Excel拆分为多个村独立档案；原始数据保留，详见本次同步批次',r['id']))
                c.execute('DELETE FROM fire_place_contact WHERE place_id=%s',(r['id'],))
                c.execute('DELETE FROM fire_place WHERE place_id=%s',(r['id'],))
                changes.append(dict(action='merge'if r['target']else 'split_summary',id=r['id'],target=r['target'],name=original_by_id[r['id']]['name']))
            actual=verify(c,plan,validation_before,manifest['schemas'])
            db.commit()
            save('applied_plan.json',plan)
            result=dict(completed=True,database='dachang',batch=batch,counts=plan['summary'],final_records=len(actual),map_records=101,source_rows_verified=101,raw_source_rows_verified=101,schemas_unchanged=True,unrelated_records_unchanged=True,changes=changes,backup=str(backup_path),coordinate_notes=[dict(name=r['name'],**r['coordinate'])for r in plan['rows']if r['coordinate']['reason']!='保留已有GCJ02坐标'])
            save('result.json',result)
            with (OUT/'同步结果_101个小区.csv').open('w',encoding='utf-8-sig',newline='')as f:
                w=csv.writer(f);w.writerow(['Excel行号','数据库ID','小区名称','所属社区','详细地址','物业公司','项目经理','联系电话','建筑面积㎡','高层栋数','经度GCJ02','纬度GCJ02','坐标依据'])
                for r in plan['rows']:w.writerow([r['row'],r['id'],r['name'],r['community'],r['address'],r['management'],r['person'],r['phone'],r['area'],r['highrise_count'],r['coordinate']['lng'],r['coordinate']['lat'],r['coordinate']['reason']])
            print(json.dumps({k:v for k,v in result.items()if k not in ['changes','coordinate_notes']},ensure_ascii=False))
    except Exception:
        db.rollback();raise
    finally:db.close()

def PathName(path):
    from pathlib import Path
    return Path(path).name

if __name__=='__main__':main()
