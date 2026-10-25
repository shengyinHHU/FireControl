-- 重点场所导入：先预检，再仅新增；不覆盖旧场所。可重复执行。
SET NAMES utf8mb4;
USE dachang;
CREATE TABLE IF NOT EXISTS fire_place_import_batch (
 batch_id varchar(36) NOT NULL PRIMARY KEY,
 owner_id bigint NOT NULL,
 file_name varchar(255) NOT NULL,
 file_sha256 char(64) NOT NULL,
 state varchar(16) NOT NULL DEFAULT 'ready',
 expires_at datetime NOT NULL,
 payload longtext NOT NULL,
 result_json longtext NULL,
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finished_at datetime NULL,
 KEY idx_place_import_owner(owner_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场所导入预检与执行结果，含来源原文';
CREATE TABLE IF NOT EXISTS fire_place_import_mutex (
 lock_id int NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场所导入串行去重锁';
INSERT IGNORE INTO fire_place_import_mutex(lock_id) VALUES(1);
-- 只新增导入权限，不扩大现有角色权限；管理员默认具有全部权限。
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT '重点场所导入',COALESCE((SELECT m.menu_id FROM (SELECT menu_id FROM sys_menu WHERE perms='fire:places:list' LIMIT 1) m),0),1,'','',1,0,'F','1','0','fire:places:import','#','admin',NOW(),'预检及仅新增导入，重复场所不覆盖'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='fire:places:import');
INSERT INTO sys_dict_data(dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT 4,'负责人','import_responsible','fire_contact_role','','default','N','0','admin',NOW(),'原表负责人，不推断为法人或主要负责人'
WHERE NOT EXISTS(SELECT 1 FROM sys_dict_data WHERE dict_type='fire_contact_role' AND dict_value='import_responsible');
INSERT INTO sys_dict_data(dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT 5,'联系人','import_contact','fire_contact_role','','default','N','0','admin',NOW(),'单页模板联系人2、3'
WHERE NOT EXISTS(SELECT 1 FROM sys_dict_data WHERE dict_type='fire_contact_role' AND dict_value='import_contact');
UPDATE sys_dict_data SET dict_label=CASE dict_value WHEN 'industry' THEN '重点企业' WHEN 'highrise' THEN '高层建筑' WHEN 'commercial' THEN '综合商业体' WHEN 'elderly' THEN '养老机构' END
WHERE dict_type='fire_place_type' AND dict_value IN ('industry','highrise','commercial','elderly');
