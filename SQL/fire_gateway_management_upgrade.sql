-- 先备份，再在已安装 fire_device_access.sql 的数据库执行；保留全部现有装置与告警。
SET NAMES utf8mb4;
USE dachang;
DROP PROCEDURE IF EXISTS fire_asset_upgrade;
DELIMITER $$
CREATE PROCEDURE fire_asset_upgrade()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND column_name='device_code') THEN ALTER TABLE fire_gateway ADD COLUMN device_code varchar(64) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND column_name='unit_name') THEN ALTER TABLE fire_gateway ADD COLUMN unit_name varchar(255) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND column_name='installation_status') THEN ALTER TABLE fire_gateway ADD COLUMN installation_status varchar(100) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND column_name='camera_ip') THEN ALTER TABLE fire_gateway ADD COLUMN camera_ip varchar(45) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND column_name='del_flag') THEN ALTER TABLE fire_gateway ADD COLUMN del_flag char(1) NOT NULL DEFAULT '0'; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND column_name='revision') THEN ALTER TABLE fire_gateway ADD COLUMN revision bigint NOT NULL DEFAULT 0; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='manufacturer') THEN ALTER TABLE fire_host ADD COLUMN manufacturer varchar(100) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='model') THEN ALTER TABLE fire_host ADD COLUMN model varchar(100) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='access_method') THEN ALTER TABLE fire_host ADD COLUMN access_method varchar(500) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='inventory_key') THEN ALTER TABLE fire_host ADD COLUMN inventory_key varchar(100) NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='del_flag') THEN ALTER TABLE fire_host ADD COLUMN del_flag char(1) NOT NULL DEFAULT '0'; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='revision') THEN ALTER TABLE fire_host ADD COLUMN revision bigint NOT NULL DEFAULT 0; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='create_by') THEN ALTER TABLE fire_host ADD COLUMN create_by varchar(64) DEFAULT ''; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_host' AND column_name='update_by') THEN ALTER TABLE fire_host ADD COLUMN update_by varchar(64) DEFAULT ''; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND index_name='uk_gateway_device_code') THEN ALTER TABLE fire_gateway ADD UNIQUE KEY uk_gateway_device_code(device_code); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='fire_host' AND index_name='uk_host_inventory') THEN ALTER TABLE fire_host ADD UNIQUE KEY uk_host_inventory(inventory_key); END IF;
 ALTER TABLE fire_gateway MODIFY place_id bigint NULL, MODIFY source_address char(12) NULL, MODIFY center_address char(12) NULL, MODIFY allowed_ip varchar(45) NULL;
 ALTER TABLE fire_host MODIFY system_type int NULL, MODIFY system_address int NULL;
END$$
DELIMITER ;
CALL fire_asset_upgrade();
DROP PROCEDURE fire_asset_upgrade;
UPDATE fire_gateway g JOIN fire_place p ON p.place_id=g.place_id SET g.unit_name=COALESCE(g.unit_name,p.name),g.dept_id=COALESCE(g.dept_id,p.dept_id),g.supervisor_user_id=COALESCE(g.supervisor_user_id,p.supervisor_user_id) WHERE g.unit_name IS NULL;
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark) SELECT '装置及下挂主机新增',0,93,'','','1','0','F','0','0','fire:hosts:add','#','admin',NOW(),'装置及主机管理；按部门/本人范围授权' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:hosts:add');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark) SELECT '装置及下挂主机修改',0,93,'','','1','0','F','0','0','fire:hosts:edit','#','admin',NOW(),'装置及主机管理；按部门/本人范围授权' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:hosts:edit');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark) SELECT '装置及下挂主机删除',0,93,'','','1','0','F','0','0','fire:hosts:remove','#','admin',NOW(),'装置及主机管理；按部门/本人范围授权' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:hosts:remove');
