-- 先备份。已安装 fire_gateway_management_upgrade.sql 后执行；重复执行安全。
-- 来源IP唯一标识一台用传；协议地址仅为报文属性，允许不同IP使用相同协议地址。
SET NAMES utf8mb4;
USE dachang;
DROP PROCEDURE IF EXISTS fire_gateway_ip_identity_upgrade;
DELIMITER $$
CREATE PROCEDURE fire_gateway_ip_identity_upgrade()
BEGIN
 IF EXISTS(SELECT allowed_ip FROM fire_gateway WHERE allowed_ip IS NOT NULL GROUP BY allowed_ip HAVING COUNT(*)>1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate gateway IPs exist; resolve inventory identities before upgrade';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND index_name='uk_fire_gateway_ip') THEN
  ALTER TABLE fire_gateway ADD UNIQUE KEY uk_fire_gateway_ip(allowed_ip);
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='fire_gateway' AND index_name='uk_fire_gateway_source') THEN
  ALTER TABLE fire_gateway DROP INDEX uk_fire_gateway_source;
 END IF;
END$$
DELIMITER ;
CALL fire_gateway_ip_identity_upgrade();
DROP PROCEDURE fire_gateway_ip_identity_upgrade;
