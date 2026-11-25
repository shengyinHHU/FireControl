import decimal,json,re
from db_support import connect,OUT,save

plan=json.loads((OUT/'applied_plan.json').read_text(encoding='utf-8'))
manifest=json.loads((OUT/'backup_manifest.json').read_text(encoding='utf-8'))
db=connect()
try:
    with db.cursor()as c:
        c.execute('START TRANSACTION READ ONLY')
        c.execute("SELECT * FROM fire_place WHERE place_type='highrise' ORDER BY place_id")
        rows=c.fetchall();assert len(rows)==101
        byname={p['name']:p for p in rows};assert len(byname)==101
        actual_map=[]
        for r in plan['rows']:
            p=byname[r['name']]
            assert p['place_id']==r['id'] and p['status']=='0' and p['del_flag']=='0'
            for col,key in [('address','address'),('community_name','community'),('management_company','management'),('industry_category','category'),('highrise_building_count','highrise_count')]:assert p[col]==r[key]
            assert p['building_area_m2']==decimal.Decimal(str(r['area']))
            assert p['coordinate_system']=='GCJ02' and p['longitude'] and p['latitude']
            c.execute("SELECT contact_name,phone FROM fire_place_contact WHERE place_id=%s AND status='0' AND del_flag='0'",(p['place_id'],))
            contacts=list(c.fetchall());assert contacts==([dict(contact_name=r['person'],phone=r['phone'])]if r['person']or r['phone']else[])
            c.execute('SELECT raw_data FROM fire_place_import_row WHERE source_key=%s',(r['source_key'],));raw=json.loads(c.fetchone()['raw_data'])
            assert raw['original_cells']==r['raw']['original_cells'] and raw['fields']==r['raw']['fields']
            actual_map.append(dict(placeId=p['place_id'],name=p['name'],placeType=p['place_type'],longitude=float(p['longitude']),latitude=float(p['latitude']),coordinateSystem=p['coordinate_system'],locationStatus=p['location_status'],address=p['address']))
        for table,expected in manifest['schemas'].items():
            c.execute('SHOW CREATE TABLE `'+table+'`');current=c.fetchone()['Create Table']
            assert re.sub(r'AUTO_INCREMENT=\d+\s*','',current)==re.sub(r'AUTO_INCREMENT=\d+\s*','',expected)
        save('map_records.json',actual_map)
        result=dict(committed_rows=len(rows),unique_names=len(byname),excel_rows_verified=len(plan['rows']),highrise_positive=sum(p['highrise_building_count']>0 for p in rows),highrise_zero=sum(p['highrise_building_count']==0 for p in rows),schema_definitions_verified=len(manifest['schemas']),coordinate_statuses=dict(__import__('collections').Counter(p['location_status']for p in rows)),outside_project_drawn_boundary=[r['name']for r in plan['rows']if not r['coordinate']['inside_project_boundary']])
        save('final_verification.json',result);print(json.dumps(result,ensure_ascii=False))
    db.rollback()
finally:db.close()
