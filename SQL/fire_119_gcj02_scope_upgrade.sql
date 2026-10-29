-- Run before starting the updated backend. Existing incidents remain intact.
-- The bounded backend worker rebuilds coordinates from original raw_json after startup.
SET NAMES utf8mb4;
USE dachang;
SET @fire119_scope_ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_incident' AND COLUMN_NAME='geofence_status')=0,'ALTER TABLE fire_119_incident ADD COLUMN geofence_status varchar(16) NOT NULL DEFAULT ''pending'' COMMENT ''纯几何范围pending/inside/outside''','SELECT 1');
PREPARE fire119_scope_stmt FROM @fire119_scope_ddl; EXECUTE fire119_scope_stmt; DEALLOCATE PREPARE fire119_scope_stmt;
SET @fire119_scope_ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_incident' AND COLUMN_NAME='geofence_version')=0,'ALTER TABLE fire_119_incident ADD COLUMN geofence_version varchar(64) NOT NULL DEFAULT '''' COMMENT ''源坐标系和转换边界版本''','SELECT 1');
PREPARE fire119_scope_stmt FROM @fire119_scope_ddl; EXECUTE fire119_scope_stmt; DEALLOCATE PREPARE fire119_scope_stmt;
SET @fire119_scope_ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_incident' AND COLUMN_NAME='boundary_distance_m')=0,'ALTER TABLE fire_119_incident ADD COLUMN boundary_distance_m decimal(12,3) DEFAULT NULL COMMENT ''距边界米数，仅提示复核''','SELECT 1');
PREPARE fire119_scope_stmt FROM @fire119_scope_ddl; EXECUTE fire119_scope_stmt; DEALLOCATE PREPARE fire119_scope_stmt;
SET @fire119_scope_ddl=IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fire_119_incident' AND INDEX_NAME='idx_119_geofence')=0,'ALTER TABLE fire_119_incident ADD INDEX idx_119_geofence(source_system,geofence_version,geofence_status,alarm_time)','SELECT 1');
PREPARE fire119_scope_stmt FROM @fire119_scope_ddl; EXECUTE fire119_scope_stmt; DEALLOCATE PREPARE fire119_scope_stmt;
