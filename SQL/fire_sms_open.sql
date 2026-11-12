-- Apply after fire_sms.sql. Repeat-safe; creates role and API metadata, never a shared password.
SET NAMES utf8mb4;
USE dachang;
CREATE TABLE IF NOT EXISTS fire_sms_open_quota (
 user_id BIGINT NOT NULL PRIMARY KEY,
 day_key CHAR(8) NOT NULL DEFAULT '', day_count INT NOT NULL DEFAULT 0,
 minute_key CHAR(12) NOT NULL DEFAULT '', minute_count INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信API按账号原子预留额度，亚洲上海自然日/分钟';
CREATE TABLE IF NOT EXISTS fire_sms_open_meta (
 sender_user_id BIGINT NOT NULL, request_id CHAR(36) NOT NULL, record_id BIGINT NOT NULL,
 biz_id VARCHAR(100) NOT NULL DEFAULT '',
 PRIMARY KEY(sender_user_id,request_id), UNIQUE KEY uk_sms_open_record(record_id),
 CONSTRAINT fk_sms_open_record FOREIGN KEY(record_id) REFERENCES fire_sms_record(record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对外短信业务编号与原始提交绑定';
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '短信开放接口·发送',0,98,'#',1,1,'F','1','0','fire:sms:open:send','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:sms:open:send');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '短信开放接口·查询',0,99,'#',1,1,'F','1','0','fire:sms:open:query','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:sms:open:query');
INSERT INTO sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
SELECT '短信接口调用者','sms_api_client',99,'5',1,1,'0','0','admin',NOW(),'仅用于短信开放接口；账号只绑定该角色'
WHERE NOT EXISTS(SELECT 1 FROM sys_role WHERE role_key='sms_api_client');
INSERT INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key='sms_api_client' AND r.del_flag='0' AND m.perms IN ('fire:sms:open:send','fire:sms:open:query')
AND NOT EXISTS(SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
