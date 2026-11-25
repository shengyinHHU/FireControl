-- 模拟警情独立存储；不写入设备报文、真实设备事件或119接入表。部署新版后端前执行。
SET NAMES utf8mb4;
USE dachang;
CREATE TABLE IF NOT EXISTS fire_alarm_simulation (
 simulation_id varchar(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 request_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 source_code varchar(16) NOT NULL,
 place_id bigint NOT NULL,
 place_name varchar(255) NOT NULL,
 address varchar(500) NOT NULL DEFAULT '',
 description varchar(500) NOT NULL,
 longitude decimal(12,8) NOT NULL,
 latitude decimal(12,8) NOT NULL,
 occurred_at datetime(3) NOT NULL,
 dept_id bigint NOT NULL,
 supervisor_user_id bigint NOT NULL,
 create_by varchar(64) NOT NULL,
 handled_at datetime(3) DEFAULT NULL,
 handled_by varchar(64) DEFAULT NULL,
 handling_note varchar(1000) DEFAULT NULL,
 PRIMARY KEY(simulation_id),
 UNIQUE KEY uk_simulation_request(supervisor_user_id,request_id),
 KEY idx_simulation_dept_time(dept_id,occurred_at),
 KEY idx_simulation_user_time(supervisor_user_id,occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人工模拟演示警情，与真实接入隔离';
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
 SELECT '模拟报警',0,94,'','','1','0','F','0','0','fire:alarms:simulate','#','admin',NOW(),'创建模拟演示警情；同时需要警情和场所查询权限'
 WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:alarms:simulate');
