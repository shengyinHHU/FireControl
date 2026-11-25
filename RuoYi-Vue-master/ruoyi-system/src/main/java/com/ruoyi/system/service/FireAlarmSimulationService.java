package com.ruoyi.system.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.domain.FirePlace;
import com.ruoyi.system.fire.Fire119Boundary;
import com.ruoyi.system.fire.FireAlarmUpdates;

/** Explicit simulation records; never fabricates a protocol report or writes to the 119 source. */
@Service
public class FireAlarmSimulationService {
 public record Create(String requestId, Long placeId, String source, String description) {}
 public record Handle(String note) {}
 private final JdbcTemplate db;
 private final IFirePlaceService places;
 private final FireAlarmQueryService alarms;
 private final Fire119Boundary boundary;
 private final FireAlarmUpdates updates;
 public FireAlarmSimulationService(DataSource data,IFirePlaceService p,FireAlarmQueryService a,Fire119Boundary b,FireAlarmUpdates u){db=new JdbcTemplate(data);places=p;alarms=a;boundary=b;updates=u;}
 private String text(String value,int max,String label){
  String s=value==null?"":value.trim();
  if(s.isEmpty()||s.length()>max)throw new ServiceException("请填写1至"+max+"字的"+label);
  return s;
 }
 private LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));}
 @Transactional
 public Map<String,Object> create(Create body){
  if(body==null||body.placeId()==null||!Set.of("device","119").contains(Objects.toString(body.source(),"")))throw new ServiceException("请选择报警来源和演示场所");
  String request=Objects.toString(body.requestId(),"");
  if(!request.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))throw new ServiceException("模拟请求编号无效，请重新打开窗口");
  String description=text(body.description(),500,"模拟报警内容");
  FirePlace q=new FirePlace();q.setPlaceId(body.placeId());
  var visible=places.selectMapPlaces(q);
  if(visible.isEmpty())throw new ServiceException("场所不存在或无权访问");
  FirePlace p=visible.get(0);
  if(!"GCJ02".equals(p.getCoordinateSystem())||!"inside".equals(boundary.match(p.getLongitude(),p.getLatitude()).status()))throw new ServiceException("请选择大厂范围内具有有效高德坐标的场所");
  long user=SecurityUtils.getUserId();
  String id="sim:"+UUID.randomUUID();
  db.update("INSERT INTO fire_alarm_simulation(simulation_id,request_id,source_code,place_id,place_name,address,description,longitude,latitude,occurred_at,dept_id,supervisor_user_id,create_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE simulation_id=simulation_id",
   id,request,body.source(),p.getPlaceId(),p.getName(),Objects.toString(p.getAddress(),""),description,p.getLongitude(),p.getLatitude(),now(),SecurityUtils.getDeptId(),user,SecurityUtils.getUsername());
  var stored=db.queryForMap("SELECT simulation_id,source_code,place_id,description FROM fire_alarm_simulation WHERE supervisor_user_id=? AND request_id=?",user,request);
  if(!body.source().equals(stored.get("source_code"))||body.placeId().longValue()!=((Number)stored.get("place_id")).longValue()||!description.equals(stored.get("description")))throw new ServiceException("此请求已生成其他内容的模拟警情，请重新打开窗口");
  updates.alarmChangedForUserAfterCommit(user);
  return Map.of("id",stored.get("simulation_id"),"isSimulation",true);
 }
 @Transactional
 public void handle(String id,Handle body){
  String note=text(body==null?null:body.note(),1000,"处理说明");
  FireAccessQuery q=new FireAccessQuery();q.setId(id);
  var scoped=alarms.alarms(q);
  if(!id.startsWith("sim:")||scoped.isEmpty()||!Boolean.TRUE.equals(scoped.get(0).get("isSimulation")))throw new ServiceException("模拟警情不存在或无权处理");
  db.update("UPDATE fire_alarm_simulation SET handled_at=?,handled_by=?,handling_note=? WHERE simulation_id=? AND handled_at IS NULL",now(),SecurityUtils.getUsername(),note,id);
  var owner=db.queryForObject("SELECT supervisor_user_id FROM fire_alarm_simulation WHERE simulation_id=?",Long.class,id);
  updates.alarmChangedForUserAfterCommit(owner);
 }
}
