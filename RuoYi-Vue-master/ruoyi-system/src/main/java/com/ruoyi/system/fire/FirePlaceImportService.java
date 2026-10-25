package com.ruoyi.system.fire;

import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import tools.jackson.databind.ObjectMapper;
import static com.ruoyi.system.fire.FirePlaceImportParser.*;

/** Durable owner-bound previews, serialized duplicate recheck, transactional insert-only commit. */
@Service
public class FirePlaceImportService {
 private final JdbcTemplate db;private final ObjectMapper json;
 public FirePlaceImportService(DataSource source,ObjectMapper mapper){db=new JdbcTemplate(source);json=mapper;}
 private String encode(Object value){return json.writeValueAsString(value);}
 private List<Map<String,Object>> names(){return db.queryForList("SELECT place_id,name FROM fire_place");}
 private void classify(List<RowData> rows,boolean admin){
  var existing=names();List<RowData> accepted=new ArrayList<>();
  for(RowData row:rows){
   if(!row.status.equals("new"))continue;
   var matches=existing.stream().filter(x->similar(row.name,String.valueOf(x.get("name")))).limit(5).toList();
   if(!matches.isEmpty()){row.status="duplicate";row.reason="数据库中已有同名或近似名称，跳过，不覆盖";row.matches=admin?matches.stream().map(x->String.valueOf(x.get("name"))).toList():List.of("已有相同或近似对象");continue;}
   var same=accepted.stream().filter(x->similar(row.name,x.name)).findFirst();
   if(same.isPresent()){row.status="duplicate";row.reason="文件内与第"+same.get().rowNumber+"行名称相同或近似，跳过";continue;}
   accepted.add(row);
  }
 }
 public static Map<String,Object> summary(List<RowData> rows){
  Map<String,Object> m=new LinkedHashMap<>();m.put("total",rows.size());
  for(String key:List.of("new","duplicate","invalid","inserted"))m.put(key,rows.stream().filter(r->r.status.equals(key)).count());
  m.put("withCoordinates",rows.stream().filter(r->Set.of("new","inserted").contains(r.status)&&r.coordinates()).count());m.put("withoutCoordinates",rows.stream().filter(r->Set.of("new","inserted").contains(r.status)&&!r.coordinates()).count());return m;
 }
 @Transactional(rollbackFor=Exception.class)
 public Map<String,Object> preview(byte[] bytes,String filename,long owner,boolean admin)throws Exception{
  var rows=parse(bytes,filename);classify(rows,admin);String id=UUID.randomUUID().toString();
  db.update("INSERT INTO fire_place_import_batch(batch_id,owner_id,file_name,file_sha256,expires_at,payload) VALUES(?,?,?,?,?,?)",id,owner,filename,hash(bytes),Timestamp.valueOf(LocalDateTime.now().plusHours(2)),encode(rows));
  return response(id,"ready",rows);
 }
 private Map<String,Object> response(String id,String state,List<RowData> rows){Map<String,Object> out=new LinkedHashMap<>();out.put("batchId",id);out.put("state",state);out.put("summary",summary(rows));out.put("rows",rows);return out;}
 @Transactional(rollbackFor=Exception.class)
 public Map<String,Object> commit(String id,long owner,Long dept,String username,boolean admin){
  if(id==null||!id.matches("[0-9a-f-]{36}"))throw new ServiceException("导入批次无效，请重新预检");
  var found=db.queryForList("SELECT * FROM fire_place_import_batch WHERE batch_id=? AND owner_id=? FOR UPDATE",id,owner);
  if(found.isEmpty())throw new ServiceException("导入批次不存在或不属于当前用户");var batch=found.get(0);
  if("complete".equals(batch.get("state")))return json.readValue(String.valueOf(batch.get("result_json")),Map.class);
  if((batch.get("expires_at") instanceof LocalDateTime time ? time : ((Timestamp)batch.get("expires_at")).toLocalDateTime()).isBefore(LocalDateTime.now()))throw new ServiceException("预检已过期，请重新上传");
  db.queryForObject("SELECT lock_id FROM fire_place_import_mutex WHERE lock_id=1 FOR UPDATE",Integer.class);
  var rows=new ArrayList<>(Arrays.asList(json.readValue(String.valueOf(batch.get("payload")),RowData[].class)));classify(rows,admin);
  for(RowData row:rows){
   if(!"new".equals(row.status))continue;
   if(db.queryForObject("SELECT COUNT(*) FROM fire_place WHERE source_key=?",Long.class,row.sourceKey)>0){row.status="duplicate";row.reason="相同来源行已导入，跳过";continue;}
   String remark=row.cells.getOrDefault("备注","");String grid=row.cells.getOrDefault("所属网格","");if(!grid.isBlank())remark+="；原所属网格："+grid+"（待关联真实网格）";if(remark.length()>500)remark=remark.substring(0,500);
   String text=remark;GeneratedKeyHolder key=new GeneratedKeyHolder();
   db.update(conn->{PreparedStatement p=conn.prepareStatement("INSERT INTO fire_place(source_key,name,place_type,industry_category,management_company,address,risk_level,dept_id,supervisor_user_id,longitude,latitude,coordinate_system,coordinate_source,location_status,create_by,remark,create_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,'GCJ02',?,?,?, ?,NOW())",Statement.RETURN_GENERATED_KEYS);
    Object[] values={row.sourceKey,row.name,row.type,blank(row,"行业类别"),blank(row,"所属/管理单位"),blank(row,"详细地址"),blank(row,"风险等级"),dept,owner,row.longitude,row.latitude,row.coordinates()?"imported":null,row.coordinates()?"review":"pending",username,text};for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);return p;},key);
   long placeId=Objects.requireNonNull(key.getKey()).longValue();
   for(int i=0;i<3;i++){String contact=row.cells.getOrDefault(i==0?"负责人":"联系人"+(i+1),"");String phone=row.cells.getOrDefault(i==0?"联系电话":"联系电话"+(i+1),"");if(contact.isBlank()&&phone.isBlank())continue;
    db.update("INSERT INTO fire_place_contact(place_id,contact_role,contact_name,phone,sort_order,create_by,create_time) VALUES(?,?,?,?,?,?,NOW())",placeId,i==0?"import_responsible":"import_contact",contact,phone,i,username);
   }
   // Existing audit schema: preserve the normalized source cells, never write source serial as place_id.
   db.update("INSERT INTO fire_place_import_row(source_key,batch_no,file_name,file_sha256,sheet_name,`row_number`,source_serial,raw_data,place_id,import_status,create_by,create_time) VALUES(?,?,?,?,?,?,?,?,?,'imported',?,NOW())",row.sourceKey,id,batch.get("file_name"),batch.get("file_sha256"),row.sheet,row.rowNumber,row.cells.getOrDefault("来源序号",""),encode(Map.of("fields",row.cells,"originalCells",row.raw)),placeId,username);
   row.status="inserted";row.reason=row.coordinates()?"已新增；有坐标，可在对应地图图层显示，定位仍待核验":"已新增；无坐标，仅在重点场所列表显示";
  }
  var result=response(id,"complete",rows);db.update("UPDATE fire_place_import_batch SET state='complete',result_json=?,finished_at=NOW() WHERE batch_id=?",encode(result),id);return result;
 }
 private static String blank(RowData r,String key){String v=r.cells.getOrDefault(key,"");return v.isBlank()?null:v;}
}
