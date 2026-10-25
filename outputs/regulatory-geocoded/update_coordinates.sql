-- 高德候选坐标；执行前审查CSV。只写待核验坐标，不连接地图或自动确认。

-- 已有坐标、已核验、停用/删除或名称地址已变更的记录均跳过。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

USE `dachang`;

START TRANSACTION;

SET @amap_updated = 0;

SELECT @amap_updated AS actual_updated_count, 0 AS generated_candidate_count;

COMMIT;
