-- Import once before configuring camera talk endpoints. No sample camera binding or password is inserted.
USE dachang;
CREATE TABLE IF NOT EXISTS fire_video_talk_endpoint (
 endpoint_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 camera_id BIGINT NOT NULL,
 host VARCHAR(45) NOT NULL,
 sdk_port INT NOT NULL DEFAULT 37777,
 credential_ref VARCHAR(64) NOT NULL DEFAULT 'default',
 enabled TINYINT NOT NULL DEFAULT 1,
 revision INT NOT NULL DEFAULT 0,
 create_by VARCHAR(64) NOT NULL DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_by VARCHAR(64) NOT NULL DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_talk_camera(camera_id), UNIQUE KEY uk_talk_address(host,sdk_port),
 CONSTRAINT fk_talk_camera FOREIGN KEY(camera_id) REFERENCES fire_video_camera(camera_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '监控对讲·连接配置',COALESCE((SELECT menu_id FROM sys_menu WHERE perms='fire:video:list' ORDER BY menu_id LIMIT 1),0),99,'#',1,1,'F','1','0','fire:video:talk-config','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:talk-config');
