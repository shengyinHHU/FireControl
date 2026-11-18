-- 重点场所增删改查升级：新增资料版本和功能权限；不覆盖现有业务资料、不自动授权角色。
SET NAMES utf8mb4;
USE dachang;
DROP PROCEDURE IF EXISTS fire_place_crud_upgrade;
DELIMITER $$
CREATE PROCEDURE fire_place_crud_upgrade()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_place' AND column_name='revision') THEN
  ALTER TABLE fire_place ADD COLUMN revision bigint NOT NULL DEFAULT 0 COMMENT '资料版本，防止覆盖并发修改';
 END IF;
END$$
DELIMITER ;
CALL fire_place_crud_upgrade();
DROP PROCEDURE fire_place_crud_upgrade;
-- 和导入复用同一把数据库锁，防止人工维护与批量导入并发新增同名资料。
CREATE TABLE IF NOT EXISTS fire_place_import_mutex(lock_id int NOT NULL PRIMARY KEY)
 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场所导入及人工维护去重锁';
INSERT IGNORE INTO fire_place_import_mutex(lock_id) VALUES(1);
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT '重点场所新增',0,95,'','',1,0,'F','0','0','fire:places:add','#','admin',NOW(),'真实场所及联系人新增；当前用户和部门归属'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:places:add');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT '重点场所编辑',0,96,'','',1,0,'F','0','0','fire:places:edit','#','admin',NOW(),'真实场所及联系人编辑；按角色数据范围控制'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:places:edit');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT '重点场所删除',0,97,'','',1,0,'F','0','0','fire:places:remove','#','admin',NOW(),'软删除场所，保留联系人、导入来源和历史关联'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:places:remove');
