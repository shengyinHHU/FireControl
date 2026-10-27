-- Run against the RuoYi database; assign permissions to the desired roles afterwards.
INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '视频监控·查看',0,90,'#',1,1,'F','1','0','fire:video:list','#','admin',SYSDATE() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:list');
INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '视频监控·播放',0,91,'#',1,1,'F','1','0','fire:video:preview','#','admin',SYSDATE() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:preview');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '视频监控·对讲',0,92,'#',1,1,'F','1','0','fire:video:talk','#','admin',SYSDATE() WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='fire:video:talk');
