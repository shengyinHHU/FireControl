-- Display-name migration only. Keep the highrise code and existing place records.
UPDATE sys_dict_data
SET dict_label = '居民小区', update_by = 'residential_label', update_time = NOW()
WHERE dict_type = 'fire_place_type' AND dict_value = 'highrise'
  AND dict_label <> '居民小区';
-- Invalidate only sys_dict:fire_place_type after applying, or restart the backend.
