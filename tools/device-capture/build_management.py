from pathlib import Path
import zipfile,xml.etree.ElementTree as E
R=Path('D:/javaprogram/FireControl')
def put(p,s):
 p=R/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def lit(v):return 'NULL' if not v else "'"+str(v).replace("'","''")+"'"
fields={'fire_gateway':{'device_code':'varchar(64) NULL','unit_name':'varchar(255) NULL','installation_status':'varchar(100) NULL','camera_ip':'varchar(45) NULL','del_flag':"char(1) NOT NULL DEFAULT '0'",'revision':'bigint NOT NULL DEFAULT 0'},'fire_host':{'manufacturer':'varchar(100) NULL','model':'varchar(100) NULL','access_method':'varchar(500) NULL','inventory_key':'varchar(100) NULL','del_flag':"char(1) NOT NULL DEFAULT '0'",'revision':'bigint NOT NULL DEFAULT 0','create_by':"varchar(64) DEFAULT ''",'update_by':"varchar(64) DEFAULT ''"}}
s='''-- 先备份，再在已安装 fire_device_access.sql 的数据库执行；保留全部现有装置与告警。
SET NAMES utf8mb4;
USE dachang;
DROP PROCEDURE IF EXISTS fire_asset_upgrade;
DELIMITER $$
CREATE PROCEDURE fire_asset_upgrade()
BEGIN
'''
for t,fs in fields.items():
 for c,d in fs.items():s+=f" IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='{t}' AND column_name='{c}') THEN ALTER TABLE {t} ADD COLUMN {c} {d}; END IF;\n"
for t,i,col in [('fire_gateway','uk_gateway_device_code','device_code'),('fire_host','uk_host_inventory','inventory_key')]:s+=f" IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='{t}' AND index_name='{i}') THEN ALTER TABLE {t} ADD UNIQUE KEY {i}({col}); END IF;\n"
s+=''' ALTER TABLE fire_gateway MODIFY place_id bigint NULL, MODIFY source_address char(12) NULL, MODIFY center_address char(12) NULL, MODIFY allowed_ip varchar(45) NULL;
 ALTER TABLE fire_host MODIFY system_type int NULL, MODIFY system_address int NULL;
END$$
DELIMITER ;
CALL fire_asset_upgrade();
DROP PROCEDURE fire_asset_upgrade;
UPDATE fire_gateway g JOIN fire_place p ON p.place_id=g.place_id SET g.unit_name=COALESCE(g.unit_name,p.name),g.dept_id=COALESCE(g.dept_id,p.dept_id),g.supervisor_user_id=COALESCE(g.supervisor_user_id,p.supervisor_user_id) WHERE g.unit_name IS NULL;
'''
for p,n in [('add','新增'),('edit','修改'),('remove','删除')]:s+=f"INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark) SELECT '装置及下挂主机{n}',0,93,'','','1','0','F','0','0','fire:hosts:{p}','#','admin',NOW(),'装置及主机管理；按部门/本人范围授权' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:hosts:{p}');\n"
put('SQL/fire_gateway_management_upgrade.sql',s)
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with zipfile.ZipFile(R/'docs/大厂街道一体化项目编号、IP表.xlsx') as z:
 ss=[''.join(n.itertext()) for n in E.fromstring(z.read('xl/sharedStrings.xml')).findall('s:si',ns)];rows=[]
 for row in E.fromstring(z.read('xl/worksheets/sheet2.xml')).findall('.//s:sheetData/s:row',ns):
  d={}
  for c in row.findall('s:c',ns):
   v=c.find('s:v',ns);v='' if v is None else v.text or ''
   if c.get('t')=='s':v=ss[int(v)]
   d[''.join(filter(str.isalpha,c.get('r')))]=v.strip()
  if d.get('C','').isdigit() and d.get('B'):rows.append(d)
assert len(rows)==36 and len({r['C'] for r in rows})==36
s='''-- 升级后导入台账：36台用传、35个单位。重复导入不覆盖人工配置。
-- 新装置停用待配置，不推测协议地址、主机系统地址、机构绑定。
-- 已连接（已测）是安装记录，不表示当前在线；摄像头IP仅作参考。
SET NAMES utf8mb4;
USE dachang;
START TRANSACTION;
'''
for r in rows:
 v=[r['C'],r['B']+'用户信息传输装置',r['B'],'浙江大华','DH-HY-FAM-1000',r.get('H'),r.get('I'),r.get('G'),r.get('J')]
 s+='INSERT INTO fire_gateway(device_code,name,unit_name,manufacturer,model,allowed_ip,camera_ip,installation_status,remark,enabled,supervisor_user_id,create_by) SELECT '+','.join(map(lit,v))+",'1',1,'inventory-import' WHERE NOT EXISTS(SELECT 1 FROM fire_gateway WHERE device_code="+lit(r['C'])+');\n'
 v=[r['B']+'消防主机',r.get('D'),r.get('E'),r.get('F'),'dachang-inventory/'+r['C']]
 s+='INSERT INTO fire_host(gateway_id,name,manufacturer,model,access_method,inventory_key,create_by) SELECT gateway_id,'+','.join(map(lit,v))+",'inventory-import' FROM fire_gateway WHERE device_code="+lit(r['C'])+' AND NOT EXISTS(SELECT 1 FROM fire_host WHERE inventory_key='+lit(v[-1])+');\n'
s+='COMMIT;\n';put('SQL/fire_gateway_inventory.sql',s)
print('Generated upgrade and 36 inventory records')
