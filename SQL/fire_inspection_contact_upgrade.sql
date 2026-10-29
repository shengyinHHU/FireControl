-- Existing installations: add optional post contacts without changing existing records.
-- Safe to run repeatedly, also safe after importing the updated fire_inspection.sql.
SET NAMES utf8mb4;
USE dachang;
SET @inspection_contact_ddl = IF(
 (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_inspection_post' AND COLUMN_NAME='contact_name')=0,
 'ALTER TABLE fire_inspection_post ADD COLUMN contact_name VARCHAR(100) NOT NULL DEFAULT '''' COMMENT ''联系人姓名''',
 'SELECT 1');
PREPARE inspection_contact_stmt FROM @inspection_contact_ddl;
EXECUTE inspection_contact_stmt;
DEALLOCATE PREPARE inspection_contact_stmt;
SET @inspection_contact_ddl = IF(
 (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_inspection_post' AND COLUMN_NAME='contact_phone')=0,
 'ALTER TABLE fire_inspection_post ADD COLUMN contact_phone VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''联系电话，保留区号和分机''',
 'SELECT 1');
PREPARE inspection_contact_stmt FROM @inspection_contact_ddl;
EXECUTE inspection_contact_stmt;
DEALLOCATE PREPARE inspection_contact_stmt;
