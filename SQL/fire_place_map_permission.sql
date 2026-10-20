-- 增量注册真实场所读取权限，不修改已有菜单，不自动给普通角色授权。
-- 超级管理员可直接访问；普通角色在若依角色管理中授予此权限并配置数据范围。
USE `dachang`;
INSERT INTO sys_menu
(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT '场所地图查询',0,0,'','',1,0,'F','1','0','fire:places:list','#','admin',NOW(),'真实场所地图、列表及详情只读权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='fire:places:list');