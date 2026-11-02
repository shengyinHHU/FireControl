-- Repeat-safe monitoring inventory. Existing video/inspection records are preserved.
USE dachang;
CREATE TABLE IF NOT EXISTS fire_video_directory (
 source_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 directory_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 parent_id VARCHAR(128) NOT NULL DEFAULT '', name VARCHAR(255) NOT NULL,
 directory_path VARCHAR(1500) NOT NULL, directory_chain VARCHAR(1500) NOT NULL, last_seen_at DATETIME NOT NULL,
 PRIMARY KEY(source_id,directory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS fire_video_camera (
 camera_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 source_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 device_code VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 device_name VARCHAR(255) NOT NULL, directory_id VARCHAR(128) NOT NULL DEFAULT '',
 directory_path VARCHAR(1500) NOT NULL DEFAULT '', directory_chain VARCHAR(1500) NOT NULL DEFAULT '', platform_status VARCHAR(64) NOT NULL DEFAULT '',
 installation_location VARCHAR(500) NOT NULL DEFAULT '', remark VARCHAR(1000) NOT NULL DEFAULT '',
 source_longitude DECIMAL(10,7) NULL, source_latitude DECIMAL(10,7) NULL, source_coord_system VARCHAR(16) NULL,
 longitude DECIMAL(10,7) NULL, latitude DECIMAL(10,7) NULL,
 location_status VARCHAR(16) NOT NULL DEFAULT 'pending', coordinate_revision INT NOT NULL DEFAULT 0,
 last_seen_at DATETIME NOT NULL, coordinate_by VARCHAR(64) NOT NULL DEFAULT '', coordinate_time DATETIME NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_video_device(source_id,device_code), KEY idx_video_location(source_id,location_status,camera_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS fire_video_sync_state (
 source_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
 job_id CHAR(36) NULL, state VARCHAR(16) NOT NULL DEFAULT 'idle',
 directories INT NOT NULL DEFAULT 0, devices INT NOT NULL DEFAULT 0,
 message VARCHAR(500) NOT NULL DEFAULT '', started_at DATETIME NULL, finished_at DATETIME NULL,
 last_success_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS fire_video_import_batch (
 batch_id CHAR(36) NOT NULL PRIMARY KEY, source_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 owner_id BIGINT NOT NULL, file_name VARCHAR(255) NOT NULL, mode VARCHAR(16) NOT NULL,
 state VARCHAR(16) NOT NULL DEFAULT 'ready', expires_at DATETIME NOT NULL,
 result_json LONGTEXT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, finished_at DATETIME NULL,
 KEY idx_video_batch_owner(owner_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS fire_video_import_row (
 batch_id CHAR(36) NOT NULL, excel_row INT NOT NULL, payload LONGTEXT NOT NULL,
 before_json LONGTEXT NULL, after_json LONGTEXT NULL, PRIMARY KEY(batch_id,excel_row),
 CONSTRAINT fk_video_import_batch FOREIGN KEY(batch_id) REFERENCES fire_video_import_batch(batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '监控目录·同步',0,95,'#',1,1,'F','1','0','fire:video:sync','#','admin',NOW() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:sync');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '监控坐标·导出',0,96,'#',1,1,'F','1','0','fire:video:export','#','admin',NOW() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:export');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '监控坐标·导入',0,97,'#',1,1,'F','1','0','fire:video:import','#','admin',NOW() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:import');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '监控坐标·编辑',0,98,'#',1,1,'F','1','0','fire:video:edit','#','admin',NOW() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:edit');
