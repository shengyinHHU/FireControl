-- MySQL 5.7.8+/8.0 增量脚本。先安装 fire_place；不删除现有数据，不自动执行。
SET NAMES utf8mb4;
USE dachang;
CREATE TABLE IF NOT EXISTS fire_gateway (
 gateway_id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
 place_id bigint NOT NULL, name varchar(255) NOT NULL,
 manufacturer varchar(100), model varchar(100),
 source_address char(12) NOT NULL, center_address char(12) NOT NULL,
 allowed_ip varchar(45) NOT NULL, enabled char(1) NOT NULL DEFAULT '0',
 dept_id bigint, supervisor_user_id bigint,
 last_received_at datetime, status_device_time datetime, status_bits int, status_description varchar(1000),
 create_by varchar(64) DEFAULT '',create_time datetime DEFAULT CURRENT_TIMESTAMP,
 update_by varchar(64) DEFAULT '',update_time datetime,remark varchar(500),
 UNIQUE KEY uk_fire_gateway_source(source_address), KEY idx_gateway_place(place_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息传输装置，enabled 0启用1停用';
CREATE TABLE IF NOT EXISTS fire_host (
 host_id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, gateway_id bigint NOT NULL,
 system_type int NOT NULL, system_address int NOT NULL, name varchar(255) NOT NULL,
 status_bits int, status_description varchar(1000), status_device_time datetime,
 last_received_at datetime, software_version varchar(30), description varchar(1000),
 create_time datetime DEFAULT CURRENT_TIMESTAMP, update_time datetime,
 UNIQUE KEY uk_fire_host_address(gateway_id,system_type,system_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际报文识别的消防系统/主机，不由装置数量虚构';
CREATE TABLE IF NOT EXISTS fire_protocol_message (
 message_id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, gateway_id bigint,
 session_id varchar(64), peer_ip varchar(45), direction char(2) NOT NULL,
 sequence_no int, command_no int, received_at datetime NOT NULL,
 device_time datetime, raw_hex mediumtext NOT NULL, parse_status varchar(32) NOT NULL,
 error_text varchar(1000), parsed_json mediumtext,
 transmission_status varchar(32) DEFAULT NULL,
 create_time datetime DEFAULT CURRENT_TIMESTAMP,
 KEY idx_message_gateway(gateway_id,received_at), KEY idx_message_session(session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐次双向原文，TX区分待发/已提交/失败，保留重传';
CREATE TABLE IF NOT EXISTS fire_event (
 event_id char(64) NOT NULL PRIMARY KEY, gateway_id bigint NOT NULL, host_id bigint,
 message_id bigint NOT NULL, object_type int NOT NULL, object_index int NOT NULL,
 object_key varchar(100) NOT NULL, event_kind varchar(32) NOT NULL,
 name varchar(1000) NOT NULL, device_time datetime, received_at datetime NOT NULL,
 clock_warning tinyint NOT NULL DEFAULT 0, details_json mediumtext NOT NULL,
 create_time datetime DEFAULT CURRENT_TIMESTAMP,
 KEY idx_event_gateway(gateway_id,received_at), KEY idx_event_kind(event_kind,received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议事件，原始对象内容哈希去重；没有模拟处置状态';
START TRANSACTION;
INSERT INTO sys_dict_data(dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT 5,'机关单位','government','fire_place_type','','info','N','0','admin',NOW(),'接入单位，不加入四类企业地图'
WHERE NOT EXISTS(SELECT 1 FROM sys_dict_data WHERE dict_type='fire_place_type' AND dict_value='government');
INSERT INTO fire_place(source_key,name,place_type,location_status,create_by,create_time,remark)
SELECT SHA2('manual/dachang-street/device-access',256),'大厂街道','government','pending','admin',NOW(),'大华装置接入单位；地址及坐标待核实'
WHERE NOT EXISTS(SELECT 1 FROM fire_place WHERE name='大厂街道' AND del_flag='0');
SET @fire_access_place_id=(SELECT place_id FROM fire_place WHERE name='大厂街道' AND del_flag='0' ORDER BY place_id LIMIT 1);
INSERT INTO fire_gateway(place_id,name,manufacturer,model,source_address,center_address,allowed_ip,dept_id,supervisor_user_id,create_by,remark)
SELECT place_id,'大厂街道用户信息传输装置','浙江大华','DH-HY-FAM-1000','c80000000000','640000000000','10.5.90.49',dept_id,supervisor_user_id,'admin','来自实机报文，未接入下级主机'
FROM fire_place WHERE place_id=@fire_access_place_id
AND NOT EXISTS(SELECT 1 FROM fire_gateway WHERE source_address='c80000000000');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT '消防接入查询',0,91,'','','1','0','F','0','0','fire:hosts:list','#','admin',NOW(),'装置及主机只读查询'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:hosts:list');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT '消防事件查询',0,92,'','','1','0','F','0','0','fire:alarms:list','#','admin',NOW(),'真实设备事件只读查询'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:alarms:list');
COMMIT;
-- 普通角色需配置功能权限及部门/本人范围。脚本不自动授予任何角色。
