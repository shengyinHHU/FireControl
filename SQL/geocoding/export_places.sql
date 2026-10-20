-- 可选：将查询结果导出为带表头的 UTF-8 CSV。程序不会直接连接数据库。
-- 导出后使用 run_geocoding.ps1 -InputFile "CSV完整路径" -Limit 101 -MaxSeconds 300。
USE `dachang`;
SELECT `place_id`, `source_key`, `name`, `address`, `normalized_address`, `place_type`
FROM `fire_place`
WHERE `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending', 'failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0'
ORDER BY `place_id`;