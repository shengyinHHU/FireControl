package com.ruoyi.system.service;
import java.util.*;
import java.text.SimpleDateFormat;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.mapper.FireAccessMapper;
import com.ruoyi.system.fire.FireConnections;

@Service
public class FireAccessService {
 private final FireAccessMapper mapper;private final FireConnections connections;
 public FireAccessService(FireAccessMapper m,FireConnections c){mapper=m;connections=c;}
 private List<Map<String,Object>> normalize(List<Map<String,Object>> rows){
  SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");fmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
  for(Map<String,Object> row:rows){
   for(var item:row.entrySet())if(item.getValue() instanceof Date)item.setValue(fmt.format((Date)item.getValue()));
   row.put("dataSource","database");
   if(row.containsKey("gatewayAddress"))row.put("status",connections.online((String)row.get("gatewayAddress"))?"在线":"离线");
   else if(row.containsKey("serial") && row.containsKey("model"))row.put("status",connections.online((String)row.get("serial"))?"在线":"离线");
  }
  return rows;
 }
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:hosts:list")
 public List<Map<String,Object>> gateways(FireAccessQuery q){return normalize(mapper.gateways(q));}
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:hosts:list")
 public List<Map<String,Object>> hosts(FireAccessQuery q){return normalize(mapper.hosts(q));}
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:alarms:list")
 public List<Map<String,Object>> events(FireAccessQuery q){return normalize(mapper.events(q));}
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:hosts:list")
 public List<Map<String,Object>> messages(FireAccessQuery q){return normalize(mapper.messages(q));}
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:hosts:list")
 public Map<String,Object> hostStats(FireAccessQuery q){
  Map<String,Object> stats=mapper.hostStats(q);stats.put("gateways",mapper.gatewayCount(q));long online=0;
  for(Map<String,Object> group:mapper.hostConnectionCounts(q))if(connections.online((String)group.get("sourceAddress")))online+=((Number)group.get("total")).longValue();
  stats.put("online",online);return stats;
 }
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:alarms:list")
 public Map<String,Object> eventStats(FireAccessQuery q){return mapper.eventStats(q);}
}
