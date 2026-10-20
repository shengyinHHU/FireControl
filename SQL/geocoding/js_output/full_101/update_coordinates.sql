-- 高德候选坐标；执行前审查CSV。只写待核验坐标，不连接地图或自动确认。

-- 已有坐标、已核验、停用/删除或名称地址已变更的记录均跳过。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

USE `dachang`;

START TRANSACTION;

SET @amap_updated = 0;

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e5b9b8e7a68fe58f8be982bbe585bbe88081e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e992a2e4b99de9be99e8b7af3233e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x33343432396562623665653630333031373735323564323537353736303661336233643335313930643732313438636362626631396335326266653635636161 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市幸福友邻养老院 | 63f55fed0f14546e0caf9269 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7420700,
  `latitude` = 32.1964250,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae58d97e992a2e4b99de9be99e8b7af3233e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e5b9b8e7a68fe58f8be982bbe585bbe88081e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e992a2e4b99de9be99e8b7af3233e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x33343432396562623665653630333031373735323564323537353736303661336233643335313930643732313438636362626631396335326266653635636161 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5ae9de6b6a6e88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e5ae9de6b6a6e88ab1e59bad31e5b9a23130322d313034e5aea4 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66326139353134396366333933343763323330363664666435306230396665393162643964333133656630376434353061316565646438643136363964336566 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市江北新区宝润老年公寓 | 757479bbdfd9e9ab4e60070a | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7347350,
  `latitude` = 32.2144710,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e5ae9de6b6a6e88ab1e59bad31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5ae9de6b6a6e88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e5ae9de6b6a6e88ab1e59bad31e5b9a23130322d313034e5aea4 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66326139353134396366333933343763323330363664666435306230396665393162643964333133656630376434353061316565646438643136363964336566 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5bfb5e5aeb6e88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae697ade4b89ce8b7af353335e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30376363373461303832613938666238336533376532616635396534323466386433323638373163326531623037633739636131393664356132623037643064 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市江北新区念家老年公寓 | 359ae1d32677416ac2e31dc2 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7591000,
  `latitude` = 32.2242100,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae697ade4b89ce8b7af353335e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5bfb5e5aeb6e88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae697ade4b89ce8b7af353335e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30376363373461303832613938666238336533376532616635396534323466386433323638373163326531623037633739636131393664356132623037643064 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5ae89e5b9b3e585bbe88081e68aa4e79086e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e6af95e6b4bce8b7af313636e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34326664363136393165623335376633366530313535376334363531633637646663666265303836373265353435653838373533373132653063633965326539 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市江北新区安平养老护理院 | 3a7eae8cfc4ad1e47939fd30 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7599540,
  `latitude` = 32.2233230,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e99587e6af95e6b4bce8b7af313636e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5ae89e5b9b3e585bbe88081e68aa4e79086e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e6af95e6b4bce8b7af313636e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34326664363136393165623335376633366530313535376334363531633637646663666265303836373265353435653838373533373132653063633965326539 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e5ae89e58f91e585bbe88081e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e6af95e6b4bce8b7af313636e58fb738e58fb7e6a5bce380813130e58fb7e6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34353037306362633730363364633131343339383661343732373333663834323231336532366164633531643637383962386439613732363861643431303833 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市安发养老有限公司 | 62bfff557a66651236469c49 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7599540,
  `latitude` = 32.2233230,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e99587e6af95e6b4bce8b7af313636e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e5ae89e58f91e585bbe88081e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e6af95e6b4bce8b7af313636e58fb738e58fb7e6a5bce380813130e58fb7e6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34353037306362633730363364633131343339383661343732373333663834323231336532366164633531643637383962386439613732363861643431303833 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae591a8e6b4bce88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e591a8e6b4bce696b0e69d913237e5b9a2e4bb98e4ba8ce6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34376137633234616431613963383635326334353761303437623232623261633338343330313861663262316662386530333861393337323665343832383361 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市江北新区周洼老年公寓 | 28ab441c51deb3c6838c7ec5 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7579030,
  `latitude` = 32.2242240,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e591a8e6b4bce696b0e69d913237e5b9a2 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae591a8e6b4bce88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e591a8e6b4bce696b0e69d913237e5b9a2e4bb98e4ba8ce6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34376137633234616431613963383635326334353761303437623232623261633338343330313861663262316662386530333861393337323665343832383361 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e5a4a7e58e82e69f8fe69da8e6a091e88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5ae8be5aeb6e5ba84313738e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x62313534303037663632646234356562306565313535643164343463616263343762386462663136653163613162346338363032353961633862386536333963 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市大厂柏杨树老年公寓 | d039581661e068a2405691f7 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7577040,
  `latitude` = 32.2117810,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5ae8be5aeb6e5ba84313738e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e5a4a7e58e82e69f8fe69da8e6a091e88081e5b9b4e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5ae8be5aeb6e5ba84313738e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x62313534303037663632646234356562306565313535643164343463616263343762386462663136653163613162346338363032353961633862386536333963 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae6b6a6e6ba90e585bbe88081e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e587a4e587b0e58d97e8b7af313538e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x38356436643337393231616538646438626237326635353133623537316531663165336233616664613130653132656530636535623164393734666436393035 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市江北新区润源养老院 | 7448bc1b1463ee18086e78cd | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7557460,
  `latitude` = 32.2094010,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe587a4e587b0e58d97e8b7af USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42303031393043505247 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae6b6a6e6ba90e585bbe88081e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e587a4e587b0e58d97e8b7af313538e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x38356436643337393231616538646438626237326635353133623537316531663165336233616664613130653132656530636535623164393734666436393035 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5928ce785a6e5a495e998b3e68aa4e585bbe999a2 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e882b2e88bb1e8b7af323939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x63633862316134376439346536333862313238336136393161656465306438653538323561373665366235303832333131393935666630663463613935616437 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市江北新区和煦夕阳护养院 | 21b0e90d11598ade50faf863 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7572680,
  `latitude` = 32.2127110,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e882b2e88bb1e8b7af323939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5928ce785a6e5a495e998b3e68aa4e585bbe999a2 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e882b2e88bb1e8b7af323939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x63633862316134376439346536333862313238336136393161656465306438653538323561373665366235303832333131393935666630663463613935616437 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae689ace5ad90e585bbe88081e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3539e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x63303865646663633732343031356334356561376564393761393631393832613465386535336234393833323331323734353864396137383632633463333432 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市江北新区扬子养老院 | 6b1afea0e369a1ce14f5b97d | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7504780,
  `latitude` = 32.2168080,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3539e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae689ace5ad90e585bbe88081e999a2 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3539e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x63303865646663633732343031356334356561376564393761393631393832613465386535336234393833323331323734353864396137383632633463333432 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5b882e8819ae7a68fe6ba90e585bbe88081e69c8de58aa1e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e587a4e587b0e58d97e8b7af3139e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x62643038373636623538653933343638336361663238326265616532393463353638643338373432643265343839353763363536346137636665636438653362 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京市聚福源养老服务有限公司 | b2e29fb72bde49adc60710c7 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7516990,
  `latitude` = 32.2059730,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e587a4e587b0e58d97e8b7af3139e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5b882e8819ae7a68fe6ba90e585bbe88081e69c8de58aa1e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e587a4e587b0e58d97e8b7af3139e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x62643038373636623538653933343638336361663238326265616532393463353638643338373432643265343839353763363536346137636665636438653362 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x656c6465726c79 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe587a4e6bba8e59889e59bad USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e587a4e6bba8e8b7af3138e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x39306336363861373336666637353730393437353435626538326263653630376464316132303465323766373036393438336236663834303661353661356564 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 凤滨嘉园 | cddef9e1267873ca7aa733f9 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7453360,
  `latitude` = 32.2052870,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe587a4e6bba8e8b7af3138e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x4230303139304355384a USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe587a4e6bba8e59889e59bad USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e587a4e6bba8e8b7af3138e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x39306336363861373336666637353730393437353435626538326263653630376464316132303465323766373036393438336236663834303661353661356564 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe6a182e9a6a8e59bad USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8a5bfe8b7af333236e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x35386637353835303236336237333965663362643336623165656231376437303733363431313666343966663139323237636230363362346332336235666332 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 桂馨园 | 6caf769ca9558fd31b9e6160 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7359800,
  `latitude` = 32.2067670,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e696b0e58d8ee8a5bfe8b7af333236e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe6a182e9a6a8e59bad USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8a5bfe8b7af333236e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x35386637353835303236336237333965663362643336623165656231376437303733363431313666343966663139323237636230363362346332336235666332 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe4b889e4b8b0e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8b7af313239e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x61373132363165626137626130353639323830346663313232643134313032626463316135363935643631653234396462323066363435316262393936656539 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 三丰公寓 | 25509ad03079ecce9ff61c70 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7476820,
  `latitude` = 32.2234300,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e696b0e58d8ee8b7af3132392d36e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe4b889e4b8b0e585ace5af93 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8b7af313239e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x61373132363165626137626130353639323830346663313232643134313032626463316135363935643631653234396462323066363435316262393936656539 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe6bb95e6b3b0e99b85e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e88d89e88ab3e8b7af313138e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31623432636639316535383434646562333834633865323365356337363633623164396265353064653431303838323633353631663639396439656537663963 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 滕泰雅苑 | 77ad0f15832b92180c84286e | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7390130,
  `latitude` = 32.2173880,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e88d89e88ab3e8b7af313138e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe6bb95e6b3b0e99b85e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e88d89e88ab3e8b7af313138e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31623432636639316535383434646562333834633865323365356337363633623164396265353064653431303838323633353631663639396439656537663963 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe68192e4b8b0e4b896e5aeb6 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e6af95e6b4bce8a5bfe8b7af323939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x61353962326561653263643138306530623261623533623763326366316361383663636461383536343966646262396663353632613337303130666265306364 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 恒丰世家 | ffcb8421954847e2ef230fa2 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7519320,
  `latitude` = 32.2320900,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e6af95e6b4bce8a5bfe8b7af3236352d323637e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe68192e4b8b0e4b896e5aeb6 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e6af95e6b4bce8a5bfe8b7af323939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x61353962326561653263643138306530623261623533623763326366316361383663636461383536343966646262396663353632613337303130666265306364 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe88da3e79b9be6b0b4e699afe59f8e USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e88d89e88ab3e8b7af323133e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x63363735666133366437336639623065636362306133366432613037346531346532326236383935376633616262383964373937646633636566393937373264 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 荣盛水景城 | cce2a8620843276b4e457623 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7400370,
  `latitude` = 32.2236740,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e88d89e88ab3e8b7af323133e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe88da3e79b9be6b0b4e699afe59f8e USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e88d89e88ab3e8b7af323133e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x63363735666133366437336639623065636362306133366432613037346531346532326236383935376633616262383964373937646633636566393937373264 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe585b0e4baade88b91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e88d89e88ab3e8b7af363030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30666132316666663034623139626633663830623038613431313437613833343662336333616563353362333133313661366639333865346235396330666632 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 兰亭苑 | 834b5c7b345317b2bd2b883c | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7472530,
  `latitude` = 32.2269340,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e88d89e88ab3e8b7af363030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe585b0e4baade88b91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e88d89e88ab3e8b7af363030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30666132316666663034623139626633663830623038613431313437613833343662336333616563353362333133313661366639333865346235396330666632 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe6b0b4e6a6ade88ab1e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af313939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x36613364393065333162656464656264663864643433626161373232313530346465323463353933373238376436663637373533353638613866643066366330 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 水榭花苑 | 72e3b8da82c2c0e3ff14fc2a | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7591880,
  `latitude` = 32.2389150,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e69da8e69d91e8b7af313939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe6b0b4e6a6ade88ab1e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e69da8e69d91e8b7af313939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x36613364393065333162656464656264663864643433626161373232313530346465323463353933373238376436663637373533353638613866643066366330 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe6aca3e59f8ee5b3b0e699af USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e6aca3e4b990e8b7af3139e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x32643737613335643035383132323963343335323432353832383731613363346166656135303134663763613063333634373736326361656465393633383961 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 欣城峰景 | 7e3c7795ac54275d4496cf97 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7545200,
  `latitude` = 32.2348130,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e6aca3e4b990e8b7af3139e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe6aca3e59f8ee5b3b0e699af USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e6aca3e4b990e8b7af3139e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x32643737613335643035383132323963343335323432353832383731613363346166656135303134663763613063333634373736326361656465393633383961 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58583e88c82e5a4a7e58ea6 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e8919be585b3e8b7af333830e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x65316631386634356166623234306335656361633631333136656161666665643161313963653339393133316139316538376235303364393930623436613464 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 元茂大厦 | b9a4a812967fa14fa0ac943a | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7613840,
  `latitude` = 32.2318110,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe8919be585b3e8b7af3338302d31e58fb74d53e68595e696afe88d9f28e5a4a7e58e82e5ba9729 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304646463657313045 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58583e88c82e5a4a7e58ea6 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e8919be585b3e8b7af333830e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x65316631386634356166623234306335656361633631333136656161666665643161313963653339393133316139316538376235303364393930623436613464 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe7bba3e699afe99b85e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e8919be585b3e8b7af373033e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x65333537333539396338383830653531663331343839643363313132353837633064643134613437643361656538616564356538636232376362363332396331 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 绣景雅苑 | 9a3786101feeea50b2ed7e02 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7519130,
  `latitude` = 32.2395420,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e8919be585b3e8b7af373033e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe7bba3e699afe99b85e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e8919be585b3e8b7af373033e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x65333537333539396338383830653531663331343839643363313132353837633064643134613437643361656538616564356538636232376362363332396331 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe993b6e6b19fe88ab1e59bad USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e59ba2e7bb93e8b7af38e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37306239373534353138343638363465663364343530616432666635646330353163383333346138663632383066356332313331333639373662353035626262 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 银江花园 | 96f3357d96cd9969af365360 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7675410,
  `latitude` = 32.2370520,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e59ba2e7bb93e8b7af38e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe993b6e6b19fe88ab1e59bad USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e59ba2e7bb93e8b7af38e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37306239373534353138343638363465663364343530616432666635646330353163383333346138663632383066356332313331333639373662353035626262 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe7a2a7e699afe5b1b1e5ba84 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af3336e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37373430373630373662623864303061386665346135613561316561346632613639313366373063623733366466313135643030656465306335383539333362 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 碧景山庄 | b919ac1aa6f646d23cf5da51 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7753320,
  `latitude` = 32.2355970,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe4b881e5aeb6e5b1b1e8b7af32e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42303031393132493141 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe7a2a7e699afe5b1b1e5ba84 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af3336e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37373430373630373662623864303061386665346135613561316561346632613639313366373063623733366466313135643030656465306335383539333362 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe697ade4b89ce696b0e59f8e USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e581a5e6b091e8b7af343030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x63376339333464393231386636353732636365626332303539303931336437363738376135333632626161623266653031383636646333663230623937663866 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 旭东新城 | 677327637a1a9e0d6d05313d | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7437920,
  `latitude` = 32.2287050,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e581a5e6b091e8b7af343030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe697ade4b89ce696b0e59f8e USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e581a5e6b091e8b7af343030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x63376339333464393231386636353732636365626332303539303931336437363738376135333632626161623266653031383636646333663230623937663866 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe5b1b1e6bd98e696b0e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e6af95e6b4bce8a5bfe8b7af3939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30666130663633333864376432393161633339656331636565623439343533313266653863643564346363333435306633386661373738643539613730356463 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 山潘新苑 | 92bc68d0b25266388d279e69 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7531240,
  `latitude` = 32.2309970,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e6af95e6b4bce8a5bfe8b7af313538e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe5b1b1e6bd98e696b0e88b91 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e6af95e6b4bce8a5bfe8b7af3939e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30666130663633333864376432393161633339656331636565623439343533313266653863643564346363333435306633386661373738643539613730356463 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe993ade69c9be5ba9c USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e5b1b1e6bd98e4b89ce8a19733e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x33653132333934363061313635646534376336653232353565343264333266613833336164313262646162663834343961373364663232666362356665663262 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 铭望府 | 6134622af2231378ce38a78c | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7568760,
  `latitude` = 32.2313940,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5b1b1e6bd98e4b89ce8a197e4b88ee9809ae6bd98e8b7afe4baa4e58f89e58fa3e8a5bf313030e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304944364d574a5558 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe993ade69c9be5ba9c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e5b1b1e6bd98e4b89ce8a19733e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x33653132333934363061313635646534376336653232353565343264333266613833336164313262646162663834343961373364663232666362356665663262 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe697b6e4bba3e88ab3e58d8e USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5b1b1e6bd98e4b89ce8a19732e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31326235333164363137326664386631326634646339316236643939356130313430396462623564626164333932643334633839356661313136303731386430 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 时代芳华 | ffc6c968b333380c2e444dbd | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7569620,
  `latitude` = 32.2296640,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe696b0e58d8ee8b7afe4b88ee6af95e6b4bce8b7afe4baa4e58f89e58fa3e58c97313630e7b1b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304a42594851484a32 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe697b6e4bba3e88ab3e58d8e USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5b1b1e6bd98e4b89ce8a19732e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31326235333164363137326664386631326634646339316236643939356130313430396462623564626164333932643334633839356661313136303731386430 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe88ab3e58d8ee5908de982b8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5b1b1e6bd98e4b89ce8a19734e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x65656330613533633134396436306365646661326231656339323763333636326366313261366633646339366538353830363366386232316331373065643038 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x6869676872697365 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 芳华名邸 | 8e10647895186ab1e07fa81d | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7580290,
  `latitude` = 32.2306480,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5b1b1e6bd98e4b89ce8a19734e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304a4b4a4147584645 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe88ab3e58d8ee5908de982b8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5b1b1e6bd98e4b89ce8a19734e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x65656330613533633134396436306365646661326231656339323763333636326366313261366633646339366538353830363366386232316331373065643038 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe4b8ade79fb3e58c96e58d97e4baace58c96e5b7a5e69cbae6a2b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a79ce6a1a531e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34333565353832393465363938373864346338663131363239653434633131643932343031383838636232613530326532613736356531396530373462396333 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 中石化南京化工机械有限公司 | 2e62cdf7c5ecd1a7f8bac64b | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7721530,
  `latitude` = 32.2266680,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a79ce6a1a531e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe4b8ade79fb3e58c96e58d97e4baace58c96e5b7a5e69cbae6a2b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a79ce6a1a531e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34333565353832393465363938373864346338663131363239653434633131643932343031383838636232613530326532613736356531396530373462396333 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace587afe88194e69c8de9a5b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66626432383931343534656635303963613831353062643233353030313764616464313331306164323635313962316435346438663365613139656433653366 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京凯联服饰有限公司 | ac1376f3ce8567f50efc31e7 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7537650,
  `latitude` = 32.2156740,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace587afe88194e69c8de9a5b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66626432383931343534656635303963613831353062643233353030313764616464313331306164323635313962316435346438663365613139656433653366 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5a4a7e58c96e69cbae5b7a5e7a88be69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae696b0e58d8ee8b7af353938e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34333266323132393431333639613964656561623139656435376265316663663261336563383837666635623164626563303633353535646538663332383464 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京大化机工程有限公司 | f19b9ae77891a6281939bab2 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7673580,
  `latitude` = 32.2340490,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae696b0e58d8ee8b7af353938e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5a4a7e58c96e69cbae5b7a5e7a88be69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae696b0e58d8ee8b7af353938e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34333266323132393431333639613964656561623139656435376265316663663261336563383837666635623164626563303633353535646538663332383464 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace58d9ae5a4a7e9878de59e8be994bbe980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a79ce6a1a5353030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34373762356339663365303939613565613335396333646363643831343234396130663563623630366433383636353638626233346662646137336137343134 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京博大重型锻造有限公司 | 96366bf56182dcf91c143cef | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7704360,
  `latitude` = 32.2293610,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a79ce6a1a5353030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace58d9ae5a4a7e9878de59e8be994bbe980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a79ce6a1a5353030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34373762356339663365303939613565613335396333646363643831343234396130663563623630366433383636353638626233346662646137336137343134 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace992a2e99381e99b86e59ba2e5bbbae8aebee69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae58db8e794b2e794b8e5b9b8e7a68fe8b7af31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x65336632323165646237643539326332363930653963666663383434343563333163326433373864616537656561346534306161356132613932353434303333 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京钢铁集团建设有限公司 | f5d0845e94d237bbc4adffb7 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7490370,
  `latitude` = 32.2001770,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae58db8e794b2e794b8e5b9b8e7a68fe8b7af31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace992a2e99381e99b86e59ba2e5bbbae8aebee69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae58db8e794b2e794b8e5b9b8e7a68fe8b7af31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x65336632323165646237643539326332363930653963666663383434343563333163326433373864616537656561346534306161356132613932353434303333 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace7a6bee8af9ae79fb3e58c96e8a385e5a487e5b7a5e7a88be69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e6af95e6b4bce8b7af313233e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37653761623230653961383762353731633138333137343464633663393031313831396137326432323838623932653362616461623530613538643539653230 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京禾诚石化装备工程有限公司 | 87bd4a9e02e2ec6441ff1b75 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7600260,
  `latitude` = 32.2257350,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e6af95e6b4bce8b7af313233e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace7a6bee8af9ae79fb3e58c96e8a385e5a487e5b7a5e7a88be69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e6af95e6b4bce8b7af313233e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37653761623230653961383762353731633138333137343464633663393031313831396137326432323838623932653362616461623530613538643539653230 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace992a2e99381e99b86e59ba2e79b9be8bebee5ae9ee4b89ae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5ae81e992a2e8b7af3639e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66363133653331653165376562616530356334393731646437333232376336386436613435366166663033373735663162393738363138316566376137346235 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京钢铁集团盛达实业有限公司 | dadd3ede77e64895e4db80a1 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7492630,
  `latitude` = 32.2015040,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5ae81e992a2e8b7af3639e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace992a2e99381e99b86e59ba2e79b9be8bebee5ae9ee4b89ae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5ae81e992a2e8b7af3639e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66363133653331653165376562616530356334393731646437333232376336386436613435366166663033373735663162393738363138316566376137346235 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe4b8ade4baa4e4b889e888aae5b180e7acace4b889e5b7a5e7a88be69c89e99990e585ace58fb8e5bbbae7ad91e5b7a5e4b89ae58886e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae6b99be6b0b4e8b7af373030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x33393461393363316232666632613635333838656165653265643930323663343734346366356537666439656361663065306239326462666163346161616665 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 中交三航局第三工程有限公司建筑工业分公司 | 69cfcbda4d23d373618eb205 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7979310,
  `latitude` = 32.2334620,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae6b99be6b0b4e8b7af373030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe4b8ade4baa4e4b889e888aae5b180e7acace4b889e5b7a5e7a88be69c89e99990e585ace58fb8e5bbbae7ad91e5b7a5e4b89ae58886e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae6b99be6b0b4e8b7af373030e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x33393461393363316232666632613635333838656165653265643930323663343734346366356537666439656361663065306239326462666163346161616665 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe6b19fe88b8fe58d97e992a2e992a2e69d90e58aa0e5b7a5e9858de98081e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5b9b8e7a68fe8b7af31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37313335303538376435323630376439623135666137643839353938373233383138643835396462346661343237373938386331323665636537643534633561 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 江苏南钢钢材加工配送有限公司 | 2f6a5c4d2edcfa351f7eb39c | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7511700,
  `latitude` = 32.2017460,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5b9b8e7a68fe8b7af31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe6b19fe88b8fe58d97e992a2e992a2e69d90e58aa0e5b7a5e9858de98081e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5b9b8e7a68fe8b7af31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37313335303538376435323630376439623135666137643839353938373233383138643835396462346661343237373938386331323665636537643534633561 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace992a2e99381e99b86e59ba2e6b19fe88b8fe586b6e98791e69cbae6a2b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae58db8e794b2e794b8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x63376430373431376161386333306230373461376136326563343531343234393132376164383661633031343866326665393661656664303732363538383261 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京钢铁集团江苏冶金机械有限公司 | b46062507ae322edc2cd414e | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7383620,
  `latitude` = 32.2072840,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae58db8e794b2e794b8 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace992a2e99381e99b86e59ba2e6b19fe88b8fe586b6e98791e69cbae6a2b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae58db8e794b2e794b8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x63376430373431376161386333306230373461376136326563343531343234393132376164383661633031343866326665393661656664303732363538383261 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5bab7e6988ee69c8de9a5b0e69c89e99990e8b4a3e4bbbbe585ace58fb8e6b0b4e6b497e58886e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae6b99be6b0b4e8b7af363638e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x61376339353731396336643234663933353936643431666662633635383234396131323362363837303166646166386432363966366431643966653735343336 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京康明服饰有限责任公司水洗分公司 | 313a94a145ba6787aa38de14 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7912900,
  `latitude` = 32.2392970,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae6b99be6b0b4e8b7af363638e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5bab7e6988ee69c8de9a5b0e69c89e99990e8b4a3e4bbbbe585ace58fb8e6b0b4e6b497e58886e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae6b99be6b0b4e8b7af363638e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x61376339353731396336643234663933353936643431666662633635383234396131323362363837303166646166386432363966366431643966653735343336 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace6b682e7919ee696b0e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e69da8e696b0e8b7af3338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x34653633376162663531653865316139626563366265633266313232613864313138306631643962663830363565633764356463386364346161303832366161 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京涂瑞新材料有限公司 | 4b1f3cd9e1b33790d32a91ff | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7309710,
  `latitude` = 32.2079010,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e69da8e696b0e8b7af3338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace6b682e7919ee696b0e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e8a197e98193e69da8e696b0e8b7af3338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x34653633376162663531653865316139626563366265633266313232613864313138306631643962663830363565633764356463386364346161303832366161 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace985b7e4bc97e69cbae6a2b0e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe4b881e5aeb6e5b1b1e8b7af323232e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37613661326335376236373962313061386436383966356666306234626638323533613266356530323335633436393963323436636663366262366662346234 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京酷众机械制造有限公司 | f8f483ea019f1bf44a699d61 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7852650,
  `latitude` = 32.2292120,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae4b881e5aeb6e5b1b1e8b7af323232e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace985b7e4bc97e69cbae6a2b0e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe4b881e5aeb6e5b1b1e8b7af323232e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37613661326335376236373962313061386436383966356666306234626638323533613266356530323335633436393963323436636663366262366662346234 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace98791e88da3e698a5e6b497e6b6a4e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe587a4e587b0e58d97e8b7af323333e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x64653532393466613432376561653237303931613831393261633136373363643339396336326562643433636231633734653732326638613131656335393133 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京金荣春洗涤有限公司 | 088151e908fdb086a9ce54c3 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7568750,
  `latitude` = 32.2094750,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = NULL,
  `amap_poi_id` = (CONVERT(0x42304c36365333425556 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace98791e88da3e698a5e6b497e6b6a4e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe587a4e587b0e58d97e8b7af323333e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x64653532393466613432376561653237303931613831393261633136373363643339396336326562643433636231633734653732326638613131656335393133 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace689ace681a9e59586e8b4b8e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af3130e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66376439373238356333323762613930303163656166613933363061613762393436353664646132353532336663633262366463306262306535333033356436 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京扬恩商贸有限公司 | deb27e34d6a4ef78302f61f1 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7786510,
  `latitude` = 32.2347650,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af3130e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace689ace681a9e59586e8b4b8e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af3130e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66376439373238356333323762613930303163656166613933363061613762393436353664646132353532336663633262366463306262306535333033356436 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace58d93e6b3bde58c96e5b7a5e8aebee5a487e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313638e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x35373230643034346330343131643963313932376662646664326264663435666264383932306331643266316665353766303839633435366639363234333835 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京卓泽化工设备有限公司 | 55e366facfe566de89163554 | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7818810,
  `latitude` = 32.2319700,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe4b881e5aeb6e5b1b1e8b7afe58d97e4baace585a8e5a4a9e9a9bee6a0a1e6aca7e6a0bce5aebee9a686e4b889e6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x423030313931354d5546 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace58d93e6b3bde58c96e5b7a5e8aebee5a487e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af313638e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x35373230643034346330343131643963313932376662646664326264663435666264383932306331643266316665353766303839633435366639363234333835 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace8af9ae5ae89e79fb3e58c96e8a385e5a487e5b7a5e7a88be69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af323232e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x33313230326265633563623736663037373339663430643031616535633132333634653531663165633361333361333534386630646362333033633539656237 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京诚安石化装备工程有限公司 | 7c2b3407ff92f4c305e1fbf8 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7852650,
  `latitude` = 32.2292120,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af323232e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace8af9ae5ae89e79fb3e58c96e8a385e5a487e5b7a5e7a88be69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e4b881e5aeb6e5b1b1e8b7af323232e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x33313230326265633563623736663037373339663430643031616535633132333634653531663165633361333361333534386630646362333033633539656237 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace79585e8bebee69cbae6a2b0e58e82 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e8919be585b3e8b7af3432e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31323135636263653735333232363235383030636664313435613230623363333732336530326633366633636666656466306366353061623935656230333636 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京畅达机械厂 | 72d342f9999124990b41828e | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7687540,
  `latitude` = 32.2227880,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5a4a7e58e82e8919be585b3e8b7af3432e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x4230303139305a305352 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace79585e8bebee69cbae6a2b0e58e82 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e8919be585b3e8b7af3432e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31323135636263653735333232363235383030636664313435613230623363333732336530326633366633636666656466306366353061623935656230333636 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace6b7bce784b1e6b688e998b2e8aebee5a487e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5928ce5b9b3e7a4bee58cbae591a8e6b4bce7bb84323537e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x38373861633061363837353237633431666662666232666634396538303433343039383134316162376234636166636163616336376433373733633762663339 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京淼焱消防设备制造有限公司 | 644be03613fc767b4ed3f41d | amap_poi

UPDATE `fire_place` SET
  `longitude` = 118.7751250,
  `latitude` = 32.2429250,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f706f69 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af313339e58fb7e5a4a7e6b48be588b6e6a1b6e58685 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = (CONVERT(0x42304a554c3730474a4a USING utf8mb4) COLLATE utf8mb4_general_ci),
  `adcode` = NULL,
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace6b7bce784b1e6b688e998b2e8aebee5a487e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5928ce5b9b3e7a4bee58cbae591a8e6b4bce7bb84323537e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x38373861633061363837353237633431666662666232666634396538303433343039383134316162376234636166636163616336376433373733633762663339 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace587afe69bbce4bbaae599a8e4bbaae8a1a8e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a79ce6a1a531e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x64393665616133643961653733343934623064396161626531656437623132373261626463626464363466323633643537666335373939353235633262623931 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京凯曼仪器仪表有限责任公司 | 1e693e60c562f0ec6fa166d0 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7721530,
  `latitude` = 32.2266680,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a79ce6a1a531e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace587afe69bbce4bbaae599a8e4bbaae8a1a8e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a79ce6a1a531e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x64393665616133643961653733343934623064396161626531656437623132373261626463626464363466323633643537666335373939353235633262623931 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace59889e58d8ee6b7b7e5879de59c9fe69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e9a9ace6b18ae6b2b3e5a4a7e7baace6a1a5e58d97e99da2 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30323663656635383964386131323266633064336438396164656333383634353933363363306263396633336335643539363961363835323531343231346131 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京嘉华混凝土有限公司 | 1a53c52e2859c0aa2f5c6ecb | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7167950,
  `latitude` = 32.2498190,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e9a9ace6b18ae6b2b3 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace59889e58d8ee6b7b7e5879de59c9fe69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e9a9ace6b18ae6b2b3e5a4a7e7baace6a1a5e58d97e99da2 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30323663656635383964386131323266633064336438396164656333383634353933363363306263396633336335643539363961363835323531343231346131 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace59ca3e587b0e588b6e8a1a3e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3434e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37303332646539663462346337613332633938336664316532666465356164633065386231613437303361623639393861316530656132616532393364393538 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京圣凰制衣有限公司 | a0f9e36311b24e03758ed406 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7495400,
  `latitude` = 32.2174970,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3434e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace59ca3e587b0e588b6e8a1a3e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3434e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37303332646539663462346337613332633938336664316532666465356164633065386231613437303361623639393861316530656132616532393364393538 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace9a39ee6b69be9a39fe59381e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x65363966346637323363636238366663656663653937366633633038653163376162326666316137346133313039336564353765613362333035386464333136 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京飞涛食品有限公司 | 9489b1517fab1a9813309e58 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7537650,
  `latitude` = 32.2156740,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace9a39ee6b69be9a39fe59381e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e5a4aae5ad90e5b1b1e8b7af3439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x65363966346637323363636238366663656663653937366633633038653163376162326666316137346133313039336564353765613362333035386464333136 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace7a791e6998be6a9a1e5a191e7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af3234362d31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31653534366564366639373632376335353837346437373964626164373639323564656461383039666461636536336563623662663237653939313538306335 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京科晋橡塑科技有限公司 | 3fc414655a23446bbde35bd0 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7797270,
  `latitude` = 32.2412650,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af3234362d31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace7a791e6998be6a9a1e5a191e7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af3234362d31e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31653534366564366639373632376335353837346437373964626164373639323564656461383039666461636536336563623662663237653939313538306335 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace6a191e699aee58a9be981aee998b3e8aebee5a487e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e585b4e8bebee8b7af3838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x61616666353535333733323232653836386564666562393163343766643333376332623462653961326535306437383365646661313238386163303336336630 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京桑普力遮阳设备制造有限公司 | 1c2cdc72b0ea733784c46b73 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7735070,
  `latitude` = 32.2346090,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e585b4e8bebee8b7af3838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace6a191e699aee58a9be981aee998b3e8aebee5a487e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e585b4e8bebee8b7af3838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x61616666353535333733323232653836386564666562393163343766643333376332623462653961326535306437383365646661313238386163303336336630 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5a5bde591b3e69da5e9a39fe59381e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e5a4aae5ad90e5b1b1e8b7af3437e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30303436646231303439366239336133316366363136663465633437393465303035316266356463393362363566353337653865303866633530393539336361 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京好味来食品有限责任公司 | 2c48958e0995919b6d264c8e | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7545830,
  `latitude` = 32.2153600,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e99587e5a4aae5ad90e5b1b1e8b7af3437e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5a5bde591b3e69da5e9a39fe59381e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e5a4aae5ad90e5b1b1e8b7af3437e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30303436646231303439366239336133316366363136663465633437393465303035316266356463393362363566353337653865303866633530393539336361 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace994a6e6b6a6e588b6e8a1a3e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e585b4e8bebee8b7af3838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66326462663262356636653264316439333866393966376632336662393135366264653330383839646230613463653065383832326362303930633161316135 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京锦润制衣有限公司 | a22e3fc7db141de21fa0ee23 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7735070,
  `latitude` = 32.2346090,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e99587e585b4e8bebee8b7af3838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace994a6e6b6a6e588b6e8a1a3e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae5a4a7e58e82e585b4e8bebee8b7af3838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66326462663262356636653264316439333866393966376632336662393135366264653330383839646230613463653065383832326362303930633161316135 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace99381e5ae8fe8bda8e98193e8bda6e8be86e8a385e5a487e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313739e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x64353534643564636136323533323864643461633939656263643361316337343737363235396135326230666330633435393865383837643133303266396263 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京铁宏轨道车辆装备制造有限公司 | 344d4e82c3ac87944e3cfc61 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7842280,
  `latitude` = 32.2304750,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313739e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace99381e5ae8fe8bda8e98193e8bda6e8be86e8a385e5a487e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313739e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x64353534643564636136323533323864643461633939656263643361316337343737363235396135326230666330633435393865383837643133303266396263 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5ae81e58d8ee5a191e69699e588b6e59381e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313739e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x37306264366636646133613334366564313562326266633733663634363532326134326339366563313266386430316333313161373236626437623466363836 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京宁华塑料制品有限公司 | 7632e8213b5a41c8444aa769 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7842280,
  `latitude` = 32.2304750,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313739e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5ae81e58d8ee5a191e69699e588b6e59381e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae4b881e5aeb6e5b1b1e8b7af313739e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x37306264366636646133613334366564313562326266633733663634363532326134326339366563313266386430316333313161373236626437623466363836 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace98791e69996e69cbae794b5e6a380e4bfaee69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae58cbae5a4a7e58e82e8a197e98193e69da8e696b0e8b7af323532e58fb72d33 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x39396164363935303363383831653661393839336638646433356239313162303335323438623038376438636234623034353239333834633062393464636364 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京金晖机电检修有限责任公司 | fa32d67e9294196412cfc376 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7367800,
  `latitude` = 32.2084000,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e8a197e98193e69da8e696b0e8b7af323532 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace98791e69996e69cbae794b5e6a380e4bfaee69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae58cbae5a4a7e58e82e8a197e98193e69da8e696b0e8b7af323532e58fb72d33 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x39396164363935303363383831653661393839336638646433356239313162303335323438623038376438636234623034353239333834633062393464636364 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace696b0e590afe6ba90e68ea2e6b58be8a385e5a487e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae69da8e696b0e8b7af333430e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x31313630336339353631333635663162323063316436643231376339623439323961346266303837346330656534323231613438373864653833396562626163 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京新启源探测装备有限责任公司 | 2f427cc3dcc6a5828d45e338 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7473330,
  `latitude` = 32.2075290,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae69da8e696b0e8b7af333430e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace696b0e590afe6ba90e68ea2e6b58be8a385e5a487e69c89e99990e8b4a3e4bbbbe585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae69da8e696b0e8b7af333430e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x31313630336339353631333635663162323063316436643231376339623439323961346266303837346330656534323231613438373864653833396562626163 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace4b999e6b08fe78eafe4bf9de699bae883bde7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae69da8e696b0e8b7af3338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x66333437633862316231383561363635643638623636323132353939626338393236353735633262626530663035303565376637366364613264383333613362 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京乙氏环保智能科技有限公司 | eb1d8c28a38de345cc5f471c | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7309710,
  `latitude` = 32.2079010,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae69da8e696b0e8b7af3338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace4b999e6b08fe78eafe4bf9de699bae883bde7a791e68a80e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b19fe58c97e696b0e58cbae69da8e696b0e8b7af3338e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x66333437633862316231383561363635643638623636323132353939626338393236353735633262626530663035303565376637366364613264383333613362 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace5ae98e59490e69cbae794b5e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e587a4e587b0e58d97e8b7af3433e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x30663437323935643362663837383133316332316431333461323336663338636133333131353937663863306132343539613830663139326334336664336535 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京官唐机电制造有限公司 | 8d208cd6dbbdf1d4fe2b26a4 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7523540,
  `latitude` = 32.2053770,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e99587e587a4e587b0e58d97e8b7af3433e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace5ae98e59490e69cbae794b5e588b6e980a0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e587a4e587b0e58d97e8b7af3433e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x30663437323935643362663837383133316332316431333461323336663338636133333131353937663863306132343539613830663139326334336664336535 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace585a8e6848fe58c85e8a385e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af323436e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x38393032323930396334383161633033646435363539656364643065643533313061636539356161326233633633383732363139386335636333343861653738 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京全意包装材料有限公司 | 6063f47d8fdb0e1947d17b90 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7792840,
  `latitude` = 32.2412980,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af323436e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace585a8e6848fe58c85e8a385e69d90e69699e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e8a197e98193e696b0e58d8ee4b89ce8b7af323436e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x38393032323930396334383161633033646435363539656364643065643533313061636539356161326233633633383732363139386335636333343861653738 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace6b6a6e58f8be586b6e98791e69cbae6a2b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae58db8e794b2e794b8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x38303265306231303830356361326339303334366562366530396664343261623833383766303865386233323964363261663435373130343363646237373933 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京润友冶金机械有限公司 | 56a6eab7a8ae381f37a8f558 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7383620,
  `latitude` = 32.2072840,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae58db8e794b2e794b8 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace6b6a6e58f8be586b6e98791e69cbae6a2b0e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe58d97e4baace5b882e6b2bfe6b19fe5b7a5e4b89ae5bc80e58f91e58cbae58db8e794b2e794b8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x38303265306231303830356361326339303334366562366530396664343261623833383766303865386233323964363261663435373130343363646237373933 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace585b1e5889be98791e5b19ee588b6e59381e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe59bade4b89ce8b7af313439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x39306164373161323364393138333730356332333539633230376332356234333638313138646438653365623832613166356362623334396332373864393036 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x696e647573747279 USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京共创金属制品有限公司 | e649e6ac212c9723f8e7af95 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7578720,
  `latitude` = 32.2227090,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae59bade4b89ce8b7af313439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace585b1e5889be98791e5b19ee588b6e59381e69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe59bade4b89ce8b7af313439e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x39306164373161323364393138333730356332333539633230376332356234333638313138646438653365623832613166356362623334396332373864393036 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe98791e5ae9de59586e4b89ae5b9bfe59cba USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe6aca3e4b990e8b7af313831e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x32633732376533366434353366326134633261373864353734623361336666353765363630333366363831616666323563353966343836343366373932333561 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 金宝商业广场 | 7bff44a898c73d4ee31292f3 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7511430,
  `latitude` = 32.2324550,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6aca3e4b990e8b7af313831e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe98791e5ae9de59586e4b89ae5b9bfe59cba USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe6aca3e4b990e8b7af313831e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x32633732376533366434353366326134633261373864353734623361336666353765363630333366363831616666323563353966343836343366373932333561 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace58a9be699aee59586e4b89ae7aea1e79086e69c89e99990e585ace58fb8efbc88e6b0b8e885bee5b9bfe59cbaefbc89 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe6b19fe58c97e696b0e58cbae5a4a7e58e82e69da8e696b0e8b7af313738e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x32333838313362333063626336313431393765346130613730303334346561333464313835363834316565636165393238633566393037316533376566336662 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京力普商业管理有限公司（永腾广场） | 782c42d23741944ab0f415e9 | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7375490,
  `latitude` = 32.2075990,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b19fe58c97e696b0e58cbae5a4a7e58e82e69d91e69da8e696b0e8b7af313738e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace58a9be699aee59586e4b89ae7aea1e79086e69c89e99990e585ace58fb8efbc88e6b0b8e885bee5b9bfe59cbaefbc89 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe6b19fe58c97e696b0e58cbae5a4a7e58e82e69da8e696b0e8b7af313738e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x32333838313362333063626336313431393765346130613730303334346561333464313835363834316565636165393238633566393037316533376566336662 USING utf8mb4) COLLATE utf8mb4_general_ci)
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

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe5a4a7e58e82e88b8fe5ae81e69893e8b4ad USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe995bfe586b2e8a1973838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x62373634653830303630666132353336616437656530623339353834303933346531623238386536333934396231633562353963313333616332636130313765 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 大厂苏宁易购 | ad12829fcd62e0290e679abe | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7643200,
  `latitude` = 32.2311740,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae995bfe586b2e8a1973838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe5a4a7e58e82e88b8fe5ae81e69893e8b4ad USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe995bfe586b2e8a1973838e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x62373634653830303630666132353336616437656530623339353834303933346531623238386536333934396231633562353963313333616332636130313765 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe4baace4b89ce4ba94e6989fe5a4a7e58e82e5ba97 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe6b0b8e588a9e8b4ade789a9e4b8ade5bf83312d33e6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x38643838316633323636383336643664303433666464623766643030373161343362336566333535336337316666646162313035626337313835363536316261 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 京东五星大厂店 | 25aec94ece2974d2ce047feb | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7629590,
  `latitude` = 32.2304740,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae6b0b8e588a9e8b4ade789a9e4b8ade5bf8331e6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe4baace4b89ce4ba94e6989fe5a4a7e58e82e5ba97 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe6b0b8e588a9e8b4ade789a9e4b8ade5bf83312d33e6a5bc USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x38643838316633323636383336643664303433666464623766643030373161343362336566333535336337316666646162313035626337313835363536316261 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE `name` = (CONVERT(0xe58d97e4baace98791e5b9b4e58d8ee5b9bfe59cbae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8b7af353238e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `source_key` = (CONVERT(0x65653033306431616361343737313539383163656165343639643166366664626364653332333330666166353564653238343137356166303061373038656464 USING utf8mb4) COLLATE utf8mb4_general_ci) AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci));

-- 南京金年华广场有限公司 | 0292151ac3a6674b9f34935f | amap_geocode

UPDATE `fire_place` SET
  `longitude` = 118.7645590,
  `latitude` = 32.2319580,
  `coordinate_system` = 'GCJ02',
  `coordinate_source` = (CONVERT(0x616d61705f67656f636f6465 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `location_status` = 'review',
  `matched_address` = (CONVERT(0xe6b19fe88b8fe79c81e58d97e4baace5b882e585ade59088e58cbae5a4a7e58e82e696b0e58d8ee8b7af353238e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `amap_poi_id` = NULL,
  `adcode` = (CONVERT(0x333230313136 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_by` = (CONVERT(0x616d61705f63616e6469646174655f32303236313032305f313130313533 USING utf8mb4) COLLATE utf8mb4_general_ci),
  `update_time` = NOW()
WHERE `place_id` = @fire_place_id
  AND `name` = (CONVERT(0xe58d97e4baace98791e5b9b4e58d8ee5b9bfe59cbae69c89e99990e585ace58fb8 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `address` <=> (CONVERT(0xe5a4a7e58e82e696b0e58d8ee8b7af353238e58fb7 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `source_key` = (CONVERT(0x65653033306431616361343737313539383163656165343639643166366664626364653332333330666166353564653238343137356166303061373038656464 USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `place_type` = (CONVERT(0x636f6d6d65726369616c USING utf8mb4) COLLATE utf8mb4_general_ci)
  AND `longitude` IS NULL AND `latitude` IS NULL
  AND `location_status` IN ('pending','failed')
  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL
  AND `status` = '0' AND `del_flag` = '0';

SET @amap_updated = @amap_updated + ROW_COUNT();

SELECT @amap_updated AS actual_updated_count, 101 AS generated_candidate_count;

COMMIT;
