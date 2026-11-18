-- 2026-11-14：仅管理传输装置。先备份，并停止旧版接收服务，再执行本脚本。
-- 保留全部传输装置、警情、原始报文及原有角色授权；删除下挂主机台账与关联列。
-- 已有安装：本脚本在管理升级、来源IP升级之后执行。可重复执行。
SET NAMES utf8mb4;
USE dachang;

START TRANSACTION;
-- 若新旧权限同时存在，把旧权限的角色关联迁移到对应新权限，避免丢失授权。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT rm.role_id,n.menu_id
FROM sys_role_menu rm JOIN sys_menu o ON o.menu_id=rm.menu_id
JOIN sys_menu n ON n.perms=REPLACE(o.perms,'fire:hosts:','fire:gateways:')
WHERE o.perms IN ('fire:hosts:list','fire:hosts:add','fire:hosts:edit','fire:hosts:remove');
DELETE rm FROM sys_role_menu rm JOIN sys_menu o ON o.menu_id=rm.menu_id
JOIN sys_menu n ON n.perms=REPLACE(o.perms,'fire:hosts:','fire:gateways:')
WHERE o.perms IN ('fire:hosts:list','fire:hosts:add','fire:hosts:edit','fire:hosts:remove');
DELETE o FROM sys_menu o JOIN sys_menu n
ON n.perms=REPLACE(o.perms,'fire:hosts:','fire:gateways:')
WHERE o.perms IN ('fire:hosts:list','fire:hosts:add','fire:hosts:edit','fire:hosts:remove');
UPDATE sys_menu SET perms=REPLACE(perms,'fire:hosts:','fire:gateways:')
WHERE perms IN ('fire:hosts:list','fire:hosts:add','fire:hosts:edit','fire:hosts:remove');
UPDATE sys_menu SET menu_name=CASE perms
 WHEN 'fire:gateways:list' THEN '传输装置查询'
 WHEN 'fire:gateways:add' THEN '传输装置新增'
 WHEN 'fire:gateways:edit' THEN '传输装置修改'
 WHEN 'fire:gateways:remove' THEN '传输装置删除' END,
 remark='传输装置管理；沿用原有部门/本人数据范围'
WHERE perms IN ('fire:gateways:list','fire:gateways:add','fire:gateways:edit','fire:gateways:remove');
UPDATE sys_menu SET path='gateways',component='fire/gateways/index',menu_name='传输装置'
WHERE component='fire/hosts/index';
COMMIT;

DROP PROCEDURE IF EXISTS fire_gateway_only_upgrade;
DELIMITER $$
CREATE PROCEDURE fire_gateway_only_upgrade()
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_event' AND column_name='host_id') THEN
  ALTER TABLE fire_event DROP COLUMN host_id;
 END IF;
END$$
DELIMITER ;
CALL fire_gateway_only_upgrade();
DROP PROCEDURE fire_gateway_only_upgrade;
DROP TABLE IF EXISTS fire_host;
