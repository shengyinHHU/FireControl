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
 @DataScope(deptAlias="g",userAlias="g",userField="supervisor_user_id",permission="fire:alarms:list")
 public List<Long> alarmGatewayIds(FireAccessQuery q){return mapper.alarmGatewayIds(q);}
 @DataScope(deptAlias="g",userAlias="g",userField="supervisor_user_id",permission="fire:gateways:list")
 public List<Long> deviceGatewayIds(FireAccessQuery q){return mapper.alarmGatewayIds(q);}
 private boolean gatewayOnline(Map<String,Object> row){
  Object enabled=row.getOrDefault("gatewayEnabled",row.get("enabled"));
  if(enabled!=null&&!"0".equals(enabled))return false;
  String ip=(String)row.getOrDefault("allowedIp",row.get("gatewayIp"));
  Object received=row.getOrDefault("gatewayLastSeen",row.get("lastSeen"));
  return connections.online(ip,received);
 }
 private List<Map<String,Object>> normalize(List<Map<String,Object>> rows){
  SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");fmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
  for(Map<String,Object> row:rows){
   boolean online=gatewayOnline(row);
   for(var item:row.entrySet())if(item.getValue() instanceof Date)item.setValue(fmt.format((Date)item.getValue()));
   row.put("dataSource","database");
   if(row.containsKey("gatewayAddress"))row.put("status",online?"在线":"离线");
   else if(row.containsKey("enabled"))row.put("status",!"0".equals(row.get("enabled"))?"停用":online?"在线":row.get("lastSeen")==null?"未收到报文":"离线");
  }
  return rows;
 }
 @DataScope(deptAlias="g",userAlias="g",userField="supervisor_user_id",permission="fire:gateways:list")
 public List<Map<String,Object>> gateways(FireAccessQuery q){return normalize(mapper.gateways(q));}
 @DataScope(deptAlias="g",userAlias="g",userField="supervisor_user_id",permission="fire:alarms:list")
 public List<Map<String,Object>> events(FireAccessQuery q){return normalize(mapper.events(q));}
 @DataScope(deptAlias="g",userAlias="g",userField="supervisor_user_id",permission="fire:gateways:list")
 public List<Map<String,Object>> messages(FireAccessQuery q){return normalize(mapper.messages(q));}
 @DataScope(deptAlias="g",userAlias="g",userField="supervisor_user_id",permission="fire:gateways:list")
 public Map<String,Object> gatewayStats(FireAccessQuery q){
  Map<String,Object> stats=new HashMap<>();stats.put("gateways",mapper.gatewayCount(q));
  long online=0,pending=0;
  for(var g:mapper.gateways(q)){if(g.get("allowedIp")==null)pending++;if(gatewayOnline(g))online++;}
  stats.put("gatewayOnline",online);stats.put("gatewayPending",pending);return stats;
 }
 @DataScope(deptAlias="g",userAlias="g",userField="supervisor_user_id",permission="fire:alarms:list")
 public Map<String,Object> eventStats(FireAccessQuery q){return mapper.eventStats(q);}
}
