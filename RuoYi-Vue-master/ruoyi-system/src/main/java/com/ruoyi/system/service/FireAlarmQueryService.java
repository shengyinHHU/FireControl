package com.ruoyi.system.service;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.mapper.Fire119Mapper;
import com.ruoyi.system.fire.Fire119Properties;

@Service
public class FireAlarmQueryService {
 private final Fire119Mapper mapper;private final Fire119Properties config;
 private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
 public FireAlarmQueryService(Fire119Mapper m,Fire119Properties p){mapper=m;config=p;}
 private void normalize(Map<String,Object> row){
  for(var entry:row.entrySet()){
   Object v=entry.getValue();
   if(v instanceof java.sql.Timestamp t)entry.setValue(t.toLocalDateTime().format(TIME));
   else if(v instanceof LocalDateTime t)entry.setValue(t.format(TIME));
   else if(v instanceof Date t)entry.setValue(t.toInstant().atZone(ZoneId.of("Asia/Shanghai")).format(TIME));
  }
  row.remove("dept_id");row.remove("supervisor_user_id");row.put("dataSource","database");
 }
 @DataScope(deptAlias="a",userAlias="a",userField="supervisor_user_id",permission="fire:alarms:list")
 public List<Map<String,Object>> alarms(FireAccessQuery q){
  q.getParams().put("sourceSystem",config.getSourceSystem());List<Map<String,Object>> rows=mapper.alarms(q);rows.forEach(this::normalize);return rows;
 }
 @DataScope(deptAlias="a",userAlias="a",userField="supervisor_user_id",permission="fire:alarms:list")
 public Map<String,Object> status(FireAccessQuery q){
  q.getParams().put("sourceSystem",config.getSourceSystem());Map<String,Object> result=mapper.scopeCounts(q);
  Map<String,Object> state=mapper.syncState(config.getSourceSystem(),config.getRegionCode());
  if(state!=null){normalize(state);result.putAll(state);}
  result.put("enabled",config.isEnabled());result.put("intervalSeconds",config.getIntervalSeconds());result.put("boundaryMode",config.getBoundaryCoordinateMode());result.put("coordinateSystem",config.getSourceCoordinateSystem());return result;
 }
}
