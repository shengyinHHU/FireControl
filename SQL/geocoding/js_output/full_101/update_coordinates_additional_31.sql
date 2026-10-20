-- 高德候选坐标；执行前审查CSV。只写待核验坐标，不连接地图或自动确认。

-- 已有坐标、已核验、停用/删除或名称地址已变更的记录均跳过。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

USE `dachang`;

START TRANSACTION;

SET @amap_updated = 0;

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe696b0e5a4a9e59cb0e59b9be5ada3e88ab1e8a197 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e58d81e69d91e696b0e5a4a9e59cb038e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x32656165336333303130643666626536666532623162366165396337393938383764356135363739353439386636363465323833343330613530336565623337 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 新天地四季花街 | 21332a93c9ab237c2ca1d44a | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7639500,
  `latitude` = 32.2305550,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe995bfe586b2e6ada5e8a18ce8a197e4b88ee8919be585b3e8b7afe4baa4e58f89e58fa3e4b89ce58c97313030e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x4230303139305a464d47 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe696b0e5a4a9e59cb0e59b9be5ada3e88ab1e8a197 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e58d81e69d91e696b0e5a4a9e59cb038e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x32656165336333303130643666626536666532623162366165396337393938383764356135363739353439386636363465323833343330613530336565623337 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe992bbe79fb3e6989fe59f8e USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e696b0e58d8ee8b7af343638e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x38356632633531633632333130383762376266303636636236666562633164346130656565643663306564353761383531346538666264396239643861633465 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 钻石星城 | e9e6b3a886bb3373114143a9 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7618100,
  `latitude` = 32.2303230,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe8919be585b3e8b7af333535e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42303031393042575143 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe992bbe79fb3e6989fe59f8e USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e696b0e58d8ee8b7af343638e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x38356632633531633632333130383762376266303636636236666562633164346130656565643663306564353761383531346538666264396239643861633465 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58c96e5bbbae696b0e4b880e69d9135332d3535 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e697ade4b89ce8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66636262333432666363353733616665303637656136656266643436393431313962643462323837363731393030386663346535333566343166613164326636 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 化建新一村53-55 | b33eb4d68c72daf39f44c4d2 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7408740,
  `latitude` = 32.2105060,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe69da8e696b0e8b7af323239e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x423030313930594a4e52 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58c96e5bbbae696b0e4b880e69d9135332d3535 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e697ade4b89ce8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66636262333432666363353733616665303637656136656266643436393431313962643462323837363731393030386663346535333566343166613164326636 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe689ace5ad9031e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66376133333538303965313234633635343334663130316438316334383938376433663163643462616438343434323861366132356462643831346162613433 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 扬子1村 | ba167e2c1423f0b1f2c62f4e | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7660000,
  `latitude` = 32.2350320,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5b9b3e998b3e8b7afe4b88ee59ba2e7bb93e8b7afe4baa4e58f89e58fa3e58d97313430e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x423046464632475a4856 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe689ace5ad9031e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66376133333538303965313234633635343334663130316438316334383938376433663163643462616438343434323861366132356462643831346162613433 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe689ace5ad9036e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x33366561663839356539313763636230663537363334653639613431323538393636613164376138303464666536343938653339326537316338303436373432 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 扬子6村 | 8149b09aee9c9f41390dfbcf | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7690130,
  `latitude` = 32.2381890,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5a4a7e58e82 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x423030313930594b3251 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe689ace5ad9036e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x33366561663839356539313763636230663537363334653639613431323538393636613164376138303464666536343938653339326537316338303436373432 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe689ace5ad903139e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x63373933623836303334326634643231376265626434623265373464366464633766393938646434393461303330643462643033336464636439373934333634 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 扬子19村 | 4951ff98d89def85de38b71c | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7607300,
  `latitude` = 32.2457710,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5b9b3e9a1b6e5b1b1e8b7afe8a5bf313530e7b1b3e9ab98e88081e5ba84e69781e8beb9 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42303031393043504a43 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe689ace5ad903139e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x63373933623836303334326634643231376265626434623265373464366464633766393938646434393461303330643462643033336464636439373934333634 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe689ace5ad903134e69d91efbc8831302d3131efbc89 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30393665323232323135636465396166613937313437383463303130653039363839323836666630343235616633306634373163313164316666356561623930 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 扬子14村（10-11） | e32eb4403bfba7d116f3aa96 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7581830,
  `latitude` = 32.2413460,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5a4a7e58e82e8a197e98193e6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cba USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304646474533535847 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe689ace5ad903134e69d91efbc8831302d3131efbc89 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30393665323232323135636465396166613937313437383463303130653039363839323836666630343235616633306634373163313164316666356561623930 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe689ace5ad903230e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x32383731386135383132626664643537316132636235633266343761383862373664353836636331343036633133613832666533646134346437313632333139 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 扬子20村 | abdbaa25c3fd6afc974a41db | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7576270,
  `latitude` = 32.2444030,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe69da8e69d91e4b880e8b7afe4b88ee689ace69d91e8b7afe4baa4e58f89e58fa3e4b89ce58c97323230e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42303031393133535333 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe689ace5ad903230e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x32383731386135383132626664643537316132636235633266343761383862373664353836636331343036633133613832666533646134346437313632333139 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe689ace5ad903134e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66353465313731333736383137613034313534646534353565663333306430363939653861626264333339333139373836396162373937626165313761373365 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 扬子14村 | 9df6d1bfa0c4e86f76a32b24 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7581830,
  `latitude` = 32.2413460,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5a4a7e58e82e8a197e98193e6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cba USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304646474533535847 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe689ace5ad903134e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66353465313731333736383137613034313534646534353565663333306430363939653861626264333339333139373836396162373937626165313761373365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe689ace5ad903231e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x38353634656631363064623735613638376461656636633861373630363763313336343262633032393032323831343133363332613737376361303066323863 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 扬子21村 | eb0e63dbe7856ba299fa8e79 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7550730,
  `latitude` = 32.2375530,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe8919be585b3e8b7af363136e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42303031393037453755 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe689ace5ad903231e69d91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x38353634656631363064623735613638376461656636633861373630363763313336343262633032393032323831343133363332613737376361303066323863 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe5b1b1e5b185e88ab1e59bad USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8a5bfe8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31303530353038396663643263393063623439653130343332373064356261356337343839306332333534393763653861396665313738633939623664636434 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 山居花园 | 390f2796e76b2f4bc1fbeea0 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7404750,
  `latitude` = 32.2059750,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe69da8e696b0e8b7afe4b88ee68588e59684e7a68fe8b7afe4baa4e58f89e58fa3e8a5bfe58d97333230e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304d55555259353232 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe5b1b1e5b185e88ab1e59bad USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8a5bfe8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31303530353038396663643263393063623439653130343332373064356261356337343839306332333534393763653861396665313738633939623664636434 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe4ba91e78ea5e7be8ee89197e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee8b7afe38081e59bade4b89ce8b7afe4baa4e58f89 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66633637323735653131333532623335316665653363666262636239626161633564373566626637333965376435373761353461366539353564626331343331 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 云玥美著苑 | 65e2384eb4c561c9f8b8ff09 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7521010,
  `latitude` = 32.2244680,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe59bade4b89ce8b7afe4b88ee696b0e58d8ee8b7afe4baa4e58f89e58fa3e4b89ce58d973535e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x423048324a5a544a3258 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe4ba91e78ea5e7be8ee89197e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee8b7afe38081e59bade4b89ce8b7afe4baa4e58f89 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66633637323735653131333532623335316665653363666262636239626161633564373566626637333965376435373761353461366539353564626331343331 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe69a84e78ea5e5ba9c USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5b1b1e6bd98e4b889e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x35346464643539343161666435653964663866316666633534363861643637363230333731343737626163386236313135663636386239363130396134336233 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 暄玥府 | 7b99d39724ac3b792cf0f7dc | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7559110,
  `latitude` = 32.2303950,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe696b0e58d8ee8b7afe4b88ee6af95e6b4bce8b7afe4baa4e58f89e58fa3e58c97323630e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304a4b5948464f3753 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe69a84e78ea5e5ba9c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5b1b1e6bd98e4b889e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x35346464643539343161666435653964663866316666633534363861643637363230333731343737626163386236313135663636386239363130396134336233 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace6a0bce98787e794b5e58a9be8aebee5a487e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x61636266653030646230373737633237666665633863323330386461356166616463633835373938646661383632343435623634326239343265313231353138 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京格采电力设备有限公司 | 4d631a7e50bb10c2c1f553d9 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7804260,
  `latitude` = 32.2258330,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace6a0bce98787e794b5e58a9be8aebee5a487e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x61636266653030646230373737633237666665633863323330386461356166616463633835373938646661383632343435623634326239343265313231353138 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace7a791e5b094e7949fe789a9e68a80e69cafe69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e4b881e5aeb6e5b1b1e8b7af313931e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x33353231303566646531383330396464623838393166326532353835383239663362306266373266333263653562643132306438313530383232613032336239 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京科尔生物技术有限公司 | f903966b37414e91cb3ae576 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7862580,
  `latitude` = 32.2287020,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af313931e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace7a791e5b094e7949fe789a9e68a80e69cafe69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e4b881e5aeb6e5b1b1e8b7af313931e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x33353231303566646531383330396464623838393166326532353835383239663362306266373266333263653562643132306438313530383232613032336239 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace9a1bae697bae69cbae794b5e7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e4b881e5aeb6e5b1b1e8b7af313931e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x35383562373335313332336632643634386238363065626438356164306437623531663839383833633332343937376636616165363836653434643736613061 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京顺旺机电科技有限公司 | 3a1430f5ee867c95f33d34b3 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7862580,
  `latitude` = 32.2287020,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af313931e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace9a1bae697bae69cbae794b5e7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e4b881e5aeb6e5b1b1e8b7af313931e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x35383562373335313332336632643634386238363065626438356164306437623531663839383833633332343937376636616165363836653434643736613061 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace58f8ce6b6a6e98791e5b19ee58aa0e5b7a5e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34646131386362643033653938363061333166303238396362356437653633353865326535643637306339663934346666366362353238323066643138646662 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京双润金属加工有限公司 | 42a2d4fec655d4484d3c0df4 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7822910,
  `latitude` = 32.2282380,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace58f8ce6b6a6e98791e5b19ee58aa0e5b7a5e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34646131386362643033653938363061333166303238396362356437653633353865326535643637306339663934346666366362353238323066643138646662 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace7b4abe78795e7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313932e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37316265343264383933323166623938633263616465343835653237353535346366353237363538653664303233396430666433396136643361393539313064 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京紫燕科技有限公司 | 5f402fea3ab9d79a9b753116 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7810460,
  `latitude` = 32.2273070,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313932e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace7b4abe78795e7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313932e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37316265343264383933323166623938633263616465343835653237353535346366353237363538653664303233396430666433396136643361393539313064 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace59889e6809de789b9e5a191e4b89ae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af313033e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34316662643032363638316537613166613138653733323539376334653434623264303338613661636264346636396236303935393331613734343932656165 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京嘉思特塑业有限公司 | 2142f63dbf573b23400f4fea | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7804170,
  `latitude` = 32.2430430,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af313033e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace59889e6809de789b9e5a191e4b89ae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af313033e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34316662643032363638316537613166613138653733323539376334653434623264303338613661636264346636396236303935393331613734343932656165 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace58d97e582b2e794b5e69cbae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31386335616563653430343132643332343936396232313431393734373930323663643834616535376238353665323461663937353739663665643637313064 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京南傲电机有限公司 | 03cda1c7315f271ab27f7ff2 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7804260,
  `latitude` = 32.2258330,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace58d97e582b2e794b5e69cbae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31386335616563653430343132643332343936396232313431393734373930323663643834616535376238353665323461663937353739663665643637313064 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe696b0e79686e5a4a7e68890e991abe6ba90e98791e5b19ee7a791e68a80e69c89e99990e585ace58fb8e58d97e4baace58886e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313639e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x36636339316462633866333638353662636636356462663661393338633835633835356564373835643938663364653735346361656465363066383561323135 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 新疆大成鑫源金属科技有限公司南京分公司 | 25bfd1c8da8cec8dd388664b | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7838830,
  `latitude` = 32.2307290,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313639e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe696b0e79686e5a4a7e68890e991abe6ba90e98791e5b19ee7a791e68a80e69c89e99990e585ace58fb8e58d97e4baace58886e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313639e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x36636339316462633866333638353662636636356462663661393338633835633835356564373835643938663364653735346361656465363066383561323135 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace7a791e6b7bce69cbae794b5e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66626163656665333637343738386265656165626663623565386139333135313932623038333966666537383035303061366666613666313732613037646437 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京科淼机电有限公司 | 14f3338bc5c7b4e8c285896f | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7822910,
  `latitude` = 32.2282380,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace7a791e6b7bce69cbae794b5e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66626163656665333637343738386265656165626663623565386139333135313932623038333966666537383035303061366666613666313732613037646437 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace99b80e5928ce69cbae794b5e8aebee5a487e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30623439613138326633343262303163373633316533663333653263666331386130346539316337353363643563373836616466383737626636626137633537 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京雀和机电设备有限公司 | 0edd95543c4b9e5c9b0d1213 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7822910,
  `latitude` = 32.2282380,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace99b80e5928ce69cbae794b5e8aebee5a487e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30623439613138326633343262303163373633316533663333653263666331386130346539316337353363643563373836616466383737626636626137633537 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe6b19fe88b8fe7a68fe5be97e5b7a5e7a88be68a80e69cafe69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66376531616130383665366431306263356332313565383537666131323139383331376138363661616365376438613830646237643164363134346138373761 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 江苏福得工程技术有限公司 | f721a745492a9435b9c84023 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7804260,
  `latitude` = 32.2258330,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe6b19fe88b8fe7a68fe5be97e5b7a5e7a88be68a80e69cafe69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66376531616130383665366431306263356332313565383537666131323139383331376138363661616365376438613830646237643164363134346138373761 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace4bc97e682a6e69cbae6a2b0e8aebee5a487e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae59490e59bbde5afbae7bb843234362d38e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34643865323338333235663338313234323137626535323065366530323139323630326166626231646530363437356535373738393865396636356237626561 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京众悦机械设备有限责任公司 | 65e7ca2cde9c8a9f65deece6 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7791380,
  `latitude` = 32.2390550,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5a4a7e58e82e696b0e58d8ee4b89ce8b7af3234362d36e69781 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x423030313931335a5942 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace4bc97e682a6e69cbae6a2b0e8aebee5a487e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae59490e59bbde5afbae7bb843234362d38e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34643865323338333235663338313234323137626535323065366530323139323630326166626231646530363437356535373738393865396636356237626561 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace88b8fe5878ce69cbae6a2b0e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae696b0e58d8ee4b89ce8b7af313339e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31633438653430303035663531393434653363636264663265653562663761333431666231366364353832636236656662333164313235623966323735303062 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京苏凌机械有限责任公司 | 7bb1aa9d3a7b504873fe18fe | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7748410,
  `latitude` = 32.2419090,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae696b0e58d8ee4b89ce8b7af313339e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace88b8fe5878ce69cbae6a2b0e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae696b0e58d8ee4b89ce8b7af313339e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31633438653430303035663531393434653363636264663265653562663761333431666231366364353832636236656662333164313235623966323735303062 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace78e96e8819ae5a48de59088e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x35366166303662363162643531386664336437333232613434306465333062393432353333323333636237383235336232386662613734633564393838356466 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京玖聚复合材料有限公司 | 09e4993251d21aba76f7c4de | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7804260,
  `latitude` = 32.2258330,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace78e96e8819ae5a48de59088e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae4b881e5aeb6e5b1b1e8b7af323032e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x35366166303662363162643531386664336437333232613434306465333062393432353333323333636237383235336232386662613734633564393838356466 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace9a9b0e7bab3e58c85e8a385e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e5928ce5b9b3e7a4bee58cbae4b881e5aeb6e5b1b1313932e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x63386236333362333730383737333864373930356637386139613638633435343135646534626266383938383366616431643162363234356237636266313438 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京驰纳包装材料有限公司 | f5a277bb377712b5c1ce60b6 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7799700,
  `latitude` = 32.2279240,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313932e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304646474f32504246 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace9a9b0e7bab3e58c85e8a385e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e5928ce5b9b3e7a4bee58cbae4b881e5aeb6e5b1b1313932e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x63386236333362333730383737333864373930356637386139613638633435343135646534626266383938383366616431643162363234356237636266313438 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace59089e6b8afe5a4a7e4bbb6e8b5b7e9878de5908ae8a385e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x61303762353663316565363461613632336334333436313562366565656231643266643530376334373739343366383162616234636666306136616330393232 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京吉港大件起重吊装有限公司 | 6b83f2d9b7d61ecc7db75a36 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7822910,
  `latitude` = 32.2282380,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace59089e6b8afe5a4a7e4bbb6e8b5b7e9878de5908ae8a385e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af313838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x61303762353663316565363461613632336334333436313562366565656231643266643530376334373739343366383162616234636666306136616330393232 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace6b0b8e588a9e8b4ade789a9e4b8ade5bf83e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e8919be585b3e8b7af323938e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66373031333733313435646538316164393462613434326632363139636265363934643537666462623164376330356336343966323239353163643738333039 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京永利购物中心有限公司 | f9b366869f72e8580340bac9 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7629180,
  `latitude` = 32.2308750,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe8919be585b3e8b7af323938e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x4230303139303939344d USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace6b0b8e588a9e8b4ade789a9e4b8ade5bf83e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e8919be585b3e8b7af323938e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66373031333733313435646538316164393462613434326632363139636265363934643537666462623164376330356336343966323239353163643738333039 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace6b7b3e4b990e59586e4b89ae7aea1e79086e69c89e99990e585ace58fb8e6b19fe58c97e58886e585ace58fb8efbc88e991abe4b990e7949fe6b4bbe5b9bfe59cbaefbc89 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe696b0e58d8ee8b7af313338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x61313237666266386332383166316566386634326134383164356536363333333261613833613066623338303234383763353237366537663638353530616136 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京淳乐商业管理有限公司江北分公司（鑫乐生活广场） | f81f5a0a02d093046c7b479c | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7470300,
  `latitude` = 32.2220770,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae696b0e58d8ee8b7af313338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace6b7b3e4b990e59586e4b89ae7aea1e79086e69c89e99990e585ace58fb8e6b19fe58c97e58886e585ace58fb8efbc88e991abe4b990e7949fe6b4bbe5b9bfe59cbaefbc89 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe696b0e58d8ee8b7af313338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x61313237666266386332383166316566386634326134383164356536363333333261613833613066623338303234383763353237366537663638353530616136 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SELECT @amap_updated AS actual_updated_count, 31 AS generated_candidate_count;

COMMIT;
