package com.ruoyi.system.fire;

import java.util.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.Fire119Mapper;
import tools.jackson.databind.ObjectMapper;

@Service
public class Fire119IngestService {
 private final Fire119Mapper mapper;private final ObjectMapper json;private final Fire119Properties config;private final Fire119Boundary boundary;
 private static final DateTimeFormatter LOCAL=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(java.time.format.ResolverStyle.STRICT);
 public Fire119IngestService(Fire119Mapper m,ObjectMapper j,Fire119Properties p,Fire119Boundary b){mapper=m;json=j;config=p;boundary=b;}
 public static LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));}
 private String encode(Object value){return json.writeValueAsString(value);}
 @SuppressWarnings("unchecked")
 public static Object canonical(Object value){
  if(value instanceof Map<?,?> m){Map<String,Object> sorted=new TreeMap<>();m.forEach((k,v)->sorted.put(String.valueOf(k),canonical(v)));return sorted;}
  if(value instanceof List<?> l)return l.stream().map(Fire119IngestService::canonical).toList();
  return value;
 }
 private String hash(Object value) throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encode(canonical(value)).getBytes(StandardCharsets.UTF_8)));}
 private static String text(Object value,int limit){if(value==null)return null;if(!(value instanceof String || value instanceof Number))throw new IllegalArgumentException("源字段类型异常");String s=String.valueOf(value);if(s.length()>limit)throw new IllegalArgumentException("源字段超过长度限制");return s;}
 private static LocalDateTime local(Object value){if(value==null||String.valueOf(value).isBlank())return null;try{return LocalDateTime.parse(String.valueOf(value),LOCAL);}catch(Exception e){return null;}}
 public static LocalDateTime utc(Object value){
  if(value==null||String.valueOf(value).isBlank())return null;
  try {String s=String.valueOf(value).replaceAll("([+-]\\d{2})(\\d{2})$","$1:$2");return OffsetDateTime.parse(s).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();}catch(Exception e){return null;}
 }
 private static Long number(Object value){if(value==null)return null;try{return new BigDecimal(String.valueOf(value)).longValueExact();}catch(Exception e){return null;}}
 private static Integer bool(Object value){if(value==null)return null;if(value instanceof Boolean b)return b?1:0;return null;}
 private static BigDecimal coordinate(Object value,int bound){try{BigDecimal b=new BigDecimal(String.valueOf(value));return b.abs().compareTo(BigDecimal.valueOf(bound))<=0?b.setScale(8,RoundingMode.HALF_UP):null;}catch(Exception e){return null;}}
 public Map<String,Object> prepare(Map<String,Object> raw,String batch) throws Exception {
  String external=text(raw.get("id"),128);if(external==null||external.isBlank())throw new IllegalArgumentException("缺少警情ID");
  String region=text(raw.get("xzqybh"),32);if(!config.getRegionCode().equals(region))throw new IllegalArgumentException("警情区划与请求不一致");
  Map<String,Object> row=new HashMap<>();row.put("source_system",config.getSourceSystem());row.put("external_id",external);row.put("order_source",config.getOrderSource());row.put("region_code",region);
  String[][] strings={{"ajlxdm","case_type_code"},{"ajlxmc","case_type_name"},{"ajztdm","case_status_code"},{"ajztmc","case_status_name"},{"afdz","address"},{"ajms","description"},{"ajxz","case_nature_code"},{"ajxzmc","case_nature_name"},{"hbzt","merge_status_code"},{"hbztmc","merge_status_name"},{"temp_ajdjdm","case_level_code"},{"temp_ajdjdmmc","case_level_name"}};
  for(String[] pair:strings)row.put(pair[1],text(raw.get(pair[0]),pair[1].endsWith("_code")?32:pair[1].endsWith("_name")?128:16000));
  row.put("alarm_time",local(raw.get("lasj")));row.put("source_sjc",local(raw.get("sjc")));
  String[][] times={{"create_time","source_create_time_utc"},{"njs_create_time","source_njs_create_time_utc"},{"njs_update_time","source_njs_update_time_utc"},{"sugon_create_time","source_sugon_create_time_utc"},{"sugon_update_time","source_sugon_update_time_utc"}};
  for(String[] pair:times)row.put(pair[1],utc(raw.get(pair[0])));
  row.put("source_manually_add",bool(raw.get("manually_add")));row.put("source_counterfeit",bool(raw.get("counterfeit")));row.put("source_flag",number(raw.get("flag")));row.put("source_manual_operate_count",number(raw.get("manual_operate_count")));
  BigDecimal lng=coordinate(raw.get("jd"),180),lat=coordinate(raw.get("wd"),90);
  if(lng==null||lat==null||(lng.signum()==0&&lat.signum()==0)){lng=null;lat=null;}
  row.put("source_longitude",lng);row.put("source_latitude",lat);row.put("source_coord_system",config.getSourceCoordinateSystem());
  boolean mapReady=lng!=null && "GCJ02".equals(config.getSourceCoordinateSystem());
  row.put("map_longitude",mapReady?lng:null);row.put("map_latitude",mapReady?lat:null);
  row.put("coordinate_status",lng==null?(raw.get("jd")==null||raw.get("wd")==null?"missing":"invalid"):mapReady?"valid":"pending");
  var match=boundary.match(lng,lat);row.put("street_name",match.status().equals("inside")?"大厂街道":null);row.put("street_match_status",match.status());row.put("street_match_method",match.method());row.put("street_match_basis",match.basis());
  row.put("raw_json",encode(raw));row.put("payload_hash",hash(raw));row.put("batch",batch);row.put("now",now());row.put("dept_id",config.getDeptId());row.put("supervisor_user_id",config.getSupervisorUserId());
  return row;
 }
 @Transactional(rollbackFor=Exception.class,timeout=15)
 @SuppressWarnings("unchecked")
 public String receive(Map<String,Object> raw,String batch) throws Exception {
  Map<String,Object> row=prepare(raw,batch);
  Map<String,Object> previous=mapper.incidentForUpdate(config.getSourceSystem(),String.valueOf(row.get("external_id")));
  String outcome;
  if(previous==null){row.put("revision_no",1L);mapper.insertIncident(row);outcome="insert";}
  else {
   row.put("incident_id",previous.get("incidentId"));
   if(Objects.equals(previous.get("payloadHash"),row.get("payload_hash"))){mapper.touchIncident(row);return "unchanged";}
   row.put("revision_no",((Number)previous.get("revisionNo")).longValue()+1);mapper.updateIncident(row);outcome="update";
  }
  row.put("change_type",outcome);
  if(previous!=null){Map<String,Object> old=json.readValue(String.valueOf(previous.get("rawJson")),Map.class);Set<String> keys=new TreeSet<>(raw.keySet());keys.addAll(old.keySet());row.put("changed_fields",encode(keys.stream().filter(k->!Objects.equals(canonical(old.get(k)),canonical(raw.get(k))) || old.containsKey(k)!=raw.containsKey(k)).toList()));}
  else row.put("changed_fields",null);
  mapper.insertHistory(row);return outcome;
 }
 @Transactional(rollbackFor=Exception.class,timeout=15)
 public void state(Map<String,Object> row){if("quick".equals(row.get("mode")))mapper.writeQuickState(row);else mapper.writeState(row);}
 @Transactional(rollbackFor=Exception.class,timeout=15)
 public void touchPage(List<Map<String,Object>> rows){
  if(!rows.isEmpty() && mapper.touchIncidents(config.getSourceSystem(),rows)!=rows.size())throw new org.springframework.dao.ConcurrencyFailureException("119记录在批量校验后发生变化，下一轮重新核对");
 }
 @Transactional(rollbackFor=Exception.class,timeout=15)
 public void log(Map<String,Object> row){mapper.insertLog(row);}
}
