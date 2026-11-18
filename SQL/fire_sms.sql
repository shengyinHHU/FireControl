-- Additive / repeat-safe. No demo recipients, account secrets, or outbound messages.
SET NAMES utf8mb4;
USE dachang;
CREATE TABLE IF NOT EXISTS fire_sms_record (
 record_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 request_id CHAR(36) NOT NULL, content_hash CHAR(64) NOT NULL, smsid VARCHAR(64) NOT NULL,
 sender_user_id BIGINT NOT NULL, sender_name VARCHAR(100) NOT NULL, dept_id BIGINT NULL,
 recipient_type VARCHAR(24) NOT NULL, source_id BIGINT NOT NULL, parent_id BIGINT NULL,
 recipient_name VARCHAR(100) NOT NULL DEFAULT '', source_name VARCHAR(255) NOT NULL DEFAULT '',
 mobiles VARCHAR(255) NOT NULL, content TEXT NOT NULL,
 task_status VARCHAR(16) NOT NULL, delivery_status VARCHAR(16) NOT NULL DEFAULT 'unconfirmed',
 platform_code VARCHAR(16) NOT NULL DEFAULT '', result_text VARCHAR(500) NOT NULL DEFAULT '',
 platform_task_id VARCHAR(128) NOT NULL DEFAULT '', accepted_phones VARCHAR(255) NOT NULL DEFAULT '',
 create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
 UNIQUE KEY uk_sms_request(sender_user_id,request_id), UNIQUE KEY uk_sms_task(smsid),
 KEY idx_sms_scope(dept_id,sender_user_id,create_time), KEY idx_sms_time(create_time,record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信提交记录，任务受理与送达状态独立';
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '短信记录·查看',0,96,'#',1,1,'F','1','0','fire:sms:list','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:sms:list');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '联系人·发送短信',COALESCE((SELECT menu_id FROM sys_menu WHERE perms='fire:sms:list' ORDER BY menu_id LIMIT 1),0),97,'#',1,1,'F','1','0','fire:sms:send','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:sms:send');
