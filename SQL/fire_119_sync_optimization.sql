-- Repeat-safe 119 quick/full synchronization state upgrade. Select the target database first.
SET @fire_119_ddl = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_sync_state' AND COLUMN_NAME='last_quick_attempt_at'),'SELECT 1','ALTER TABLE fire_119_sync_state ADD COLUMN `last_quick_attempt_at` datetime(3) DEFAULT NULL');
PREPARE fire_119_upgrade FROM @fire_119_ddl; EXECUTE fire_119_upgrade; DEALLOCATE PREPARE fire_119_upgrade;
SET @fire_119_ddl = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_sync_state' AND COLUMN_NAME='last_quick_completed_at'),'SELECT 1','ALTER TABLE fire_119_sync_state ADD COLUMN `last_quick_completed_at` datetime(3) DEFAULT NULL');
PREPARE fire_119_upgrade FROM @fire_119_ddl; EXECUTE fire_119_upgrade; DEALLOCATE PREPARE fire_119_upgrade;
SET @fire_119_ddl = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_sync_state' AND COLUMN_NAME='last_quick_result'),'SELECT 1','ALTER TABLE fire_119_sync_state ADD COLUMN `last_quick_result` varchar(16) NOT NULL DEFAULT ''never''');
PREPARE fire_119_upgrade FROM @fire_119_ddl; EXECUTE fire_119_upgrade; DEALLOCATE PREPARE fire_119_upgrade;
SET @fire_119_ddl = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_sync_state' AND COLUMN_NAME='last_quick_error'),'SELECT 1','ALTER TABLE fire_119_sync_state ADD COLUMN `last_quick_error` varchar(2000) DEFAULT NULL');
PREPARE fire_119_upgrade FROM @fire_119_ddl; EXECUTE fire_119_upgrade; DEALLOCATE PREPARE fire_119_upgrade;
SET @fire_119_ddl = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_sync_state' AND COLUMN_NAME='last_quick_failures'),'SELECT 1','ALTER TABLE fire_119_sync_state ADD COLUMN `last_quick_failures` int unsigned NOT NULL DEFAULT 0');
PREPARE fire_119_upgrade FROM @fire_119_ddl; EXECUTE fire_119_upgrade; DEALLOCATE PREPARE fire_119_upgrade;
SET @fire_119_ddl = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_sync_state' AND COLUMN_NAME='last_new_incident_at'),'SELECT 1','ALTER TABLE fire_119_sync_state ADD COLUMN `last_new_incident_at` datetime(3) DEFAULT NULL');
PREPARE fire_119_upgrade FROM @fire_119_ddl; EXECUTE fire_119_upgrade; DEALLOCATE PREPARE fire_119_upgrade;
