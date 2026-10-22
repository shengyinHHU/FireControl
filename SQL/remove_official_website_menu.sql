-- Remove only the obsolete external official-website menu and its role grants.
START TRANSACTION;
DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id=rm.menu_id
WHERE m.menu_id=4 AND m.menu_name='扬子石化官网' AND m.path='http://ruoyi.vip';
DELETE FROM sys_menu WHERE menu_id=4 AND menu_name='扬子石化官网' AND path='http://ruoyi.vip';
COMMIT;
