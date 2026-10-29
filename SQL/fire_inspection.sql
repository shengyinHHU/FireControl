-- Incremental schema only; no demo/business data and no modifications to fire_place/sys_post.
SET NAMES utf8mb4;
USE dachang;
CREATE TABLE IF NOT EXISTS fire_inspection_post (
 post_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 post_name VARCHAR(160) NOT NULL, place_id BIGINT NULL,
 contact_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '联系人姓名',
 contact_phone VARCHAR(64) NOT NULL DEFAULT '' COMMENT '联系电话，保留区号和分机',
 dept_id BIGINT NULL, supervisor_user_id BIGINT NOT NULL,
 status CHAR(1) NOT NULL DEFAULT '0', sort_order INT NOT NULL DEFAULT 0,
 revision INT NOT NULL DEFAULT 1, remark VARCHAR(500) NOT NULL DEFAULT '',
 create_by VARCHAR(64) NOT NULL, create_time DATETIME NOT NULL,
 update_by VARCHAR(64) NOT NULL, update_time DATETIME NOT NULL,
 KEY idx_inspection_post_scope(dept_id,supervisor_user_id), KEY idx_inspection_post_status(status,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际视频查岗岗位，非若依人员岗位';
CREATE TABLE IF NOT EXISTS fire_inspection_post_camera (
 binding_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, post_id BIGINT NOT NULL,
 video_source VARCHAR(64) NOT NULL DEFAULT 'ctseelink-default',
 device_code VARCHAR(128) NOT NULL, device_name VARCHAR(255) NOT NULL,
 is_primary TINYINT NOT NULL DEFAULT 0, sort_order INT NOT NULL DEFAULT 0,
 status CHAR(1) NOT NULL DEFAULT '0', create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
 UNIQUE KEY uk_inspection_binding(post_id,video_source,device_code),
 KEY idx_inspection_binding_post(post_id,status,sort_order),
 CONSTRAINT fk_inspection_camera_post FOREIGN KEY(post_id) REFERENCES fire_inspection_post(post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位摄像头绑定，解绑停用保留追溯';
CREATE TABLE IF NOT EXISTS fire_inspection_record (
 record_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, request_id CHAR(36) NOT NULL,
 post_id BIGINT NOT NULL, binding_id BIGINT NOT NULL, place_id BIGINT NULL,
 post_name VARCHAR(160) NOT NULL, place_name VARCHAR(255) NOT NULL DEFAULT '',
 video_source VARCHAR(64) NOT NULL, device_code VARCHAR(128) NOT NULL, device_name VARCHAR(255) NOT NULL,
 inspector_user_id BIGINT NOT NULL, inspector_name VARCHAR(100) NOT NULL,
 dept_id BIGINT NULL, supervisor_user_id BIGINT NOT NULL,
 result VARCHAR(16) NOT NULL, note VARCHAR(1000) NOT NULL DEFAULT '',
 evidence_file VARCHAR(64) NULL, evidence_sha256 CHAR(64) NULL, evidence_size INT NULL,
 content_hash CHAR(64) NOT NULL, inspect_time DATETIME NOT NULL,
 UNIQUE KEY uk_inspection_request(request_id),
 KEY idx_inspection_record_post(post_id,inspect_time,record_id),
 KEY idx_inspection_record_scope(dept_id,supervisor_user_id,inspect_time),
 CONSTRAINT fk_inspection_record_post FOREIGN KEY(post_id) REFERENCES fire_inspection_post(post_id),
 CONSTRAINT fk_inspection_record_binding FOREIGN KEY(binding_id) REFERENCES fire_inspection_post_camera(binding_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='真实人工查岗记录及当时名称快照';

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '查岗岗位·查看',0,93,'#',1,1,'F','1','0','fire:inspections:list','#','admin',NOW() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:inspections:list');

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '查岗岗位·配置',0,94,'#',1,1,'F','1','0','fire:inspections:edit','#','admin',NOW() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:inspections:edit');

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '查岗记录·新增',0,95,'#',1,1,'F','1','0','fire:inspections:add','#','admin',NOW() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:inspections:add');
