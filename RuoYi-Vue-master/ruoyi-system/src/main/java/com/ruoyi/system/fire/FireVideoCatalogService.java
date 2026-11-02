package com.ruoyi.system.fire;

import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import com.ruoyi.common.exception.ServiceException;
import tools.jackson.databind.ObjectMapper;
import static com.ruoyi.system.fire.FireVideoCoordinateExcel.*;

/** Account-scoped persistent catalog; platform sync never writes surveyed coordinates. */
@Service
public class FireVideoCatalogService {
 private final JdbcTemplate db;private final FireVideoClient client;private final ObjectMapper json;private final TransactionTemplate tx;
 private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"video-catalog-sync");t.setDaemon(true);return t;});
 public FireVideoCatalogService(DataSource ds,FireVideoClient client,ObjectMapper json,PlatformTransactionManager manager){db=new JdbcTemplate(ds);this.client=client;this.json=json;tx=new TransactionTemplate(manager);}
 @PreDestroy public void close(){worker.shutdownNow();}
 private static final String SELECT="SELECT CAST(camera_id AS CHAR) AS id,source_id AS source,device_code AS deviceCode,device_name AS name,device_name AS deviceName,directory_id AS directoryId,directory_path AS directoryPath,platform_status AS platformStatus,installation_location AS installationLocation,remark,longitude AS lng,latitude AS lat,source_longitude AS sourceLongitude,source_latitude AS sourceLatitude,source_coord_system AS sourceCoordinateSystem,location_status AS locationStatus,coordinate_revision AS revision,last_seen_at AS lastSeenAt,coordinate_by AS coordinateBy,coordinate_time AS coordinateTime FROM fire_video_camera";
 public record Directory(String id,String parentId,String name,String path,String chain){}
 public record Device(String code,String name,String directoryId,String path,String chain,String status){}
 public record Snapshot(List<Directory> directories,List<Device> devices){}
 @FunctionalInterface public interface Progress {void update(int directories,int devices);}
 private static String text(Object value,int max){String s=value==null?"":String.valueOf(value);if(s.length()>max)throw new ServiceException("平台返回字段超过允许长度");return s;}
 public static Snapshot collect(FireVideoClient client,Progress progress)throws Exception{
  var dirs=new LinkedHashMap<String,Directory>();var devices=new LinkedHashMap<String,Device>();var queue=new ArrayDeque<Directory>();
  queue.add(new Directory("","","","","/"));long deadline=System.currentTimeMillis()+600000;int pages=0;
  while(!queue.isEmpty()){
   if(Thread.currentThread().isInterrupted()||System.currentTimeMillis()>deadline)throw new ServiceException("同步超时，已保留原台账，请重试");
   Directory parent=queue.remove();Long region=parent.id.isEmpty()?null:Long.valueOf(parent.id);
   Object nested=client.regions(region);if(!(nested instanceof List<?> children))throw new ServiceException("目录返回格式异常，同步未提交");
   for(Object value:children){
    if(!(value instanceof Map<?,?> row))throw new ServiceException("目录条目格式异常");String id=text(row.get("id"),128),name=text(row.get("name"),255);
    if(!id.matches("\\d{1,18}")||name.isBlank())throw new ServiceException("目录编码或名称无效");
    if(dirs.containsKey(id))throw new ServiceException("目录重复或循环，同步未提交");
    String path=parent.path.isEmpty()?name:parent.path+" / "+name,chain=parent.chain+id+"/";
    if(path.length()>1500||chain.length()>1500||dirs.size()>=5000)throw new ServiceException("目录层级或数量超过同步上限");
    var dir=new Directory(id,parent.id,name,path,chain);dirs.put(id,dir);queue.add(dir);
   }
   long cursor=-1;Set<Long> cursors=new HashSet<>();
   while(true){
    if(++pages>10000||System.currentTimeMillis()>deadline||Thread.currentThread().isInterrupted())throw new ServiceException("同步超过限制，原台账保持不变");
    Object result=client.devices(region,cursor,100);if(!(result instanceof Map<?,?> page)||!(page.get("list") instanceof List<?> rows)||page.get("lastId")==null)throw new ServiceException("设备分页返回格式异常，同步未提交");
    for(Object value:rows){if(!(value instanceof Map<?,?> row))throw new ServiceException("设备条目格式异常");String code=text(row.get("deviceCode"),128),name=text(row.get("deviceName"),255);
     if(!code.matches("[A-Za-z0-9_.:-]{1,128}"))throw new ServiceException("平台设备编码无效");
     var old=devices.get(code);if(old==null||old.directoryId.isEmpty())devices.put(code,new Device(code,name.isBlank()?"未命名摄像头":name,parent.id,parent.path,parent.chain,text(row.get("status"),64)));
     if(devices.size()>MAX_ROWS)throw new ServiceException("监控超过50000台，请联系管理员调整容量");
    }
    progress.update(dirs.size(),devices.size());long next;try{next=Long.parseLong(String.valueOf(page.get("lastId")));}catch(Exception invalid){throw new ServiceException("设备分页游标无效");}
    if(next<0)break;if(next==cursor||!cursors.add(next))throw new ServiceException("设备分页游标重复，同步未提交");cursor=next;
   }
  }
  return new Snapshot(new ArrayList<>(dirs.values()),new ArrayList<>(devices.values()));
 }
 public Map<String,Object> startSync(){
  String source=client.sourceId(),job=UUID.randomUUID().toString();
  db.update("INSERT IGNORE INTO fire_video_sync_state(source_id) VALUES(?)",source);
  int changed=db.update("UPDATE fire_video_sync_state SET job_id=?,state='running',directories=0,devices=0,message='正在读取平台目录和全部分页',started_at=NOW(),finished_at=NULL WHERE source_id=? AND (state<>'running' OR started_at<DATE_SUB(NOW(),INTERVAL 15 MINUTE))",job,source);
  if(changed==0)throw new ServiceException("已有同步任务运行，请等待完成");
  try{worker.execute(new DelegatingSecurityContextRunnable(()->{
   try{Snapshot snapshot=collect(client,(d,c)->db.update("UPDATE fire_video_sync_state SET directories=?,devices=? WHERE source_id=? AND job_id=?",d,c,source,job));applySnapshot(source,snapshot,job);}
   catch(Exception failure){
    String reason="同步任务失败，请联系管理员";
    if(failure instanceof ServiceException){reason=failure.getMessage();if(reason.startsWith("视频平台"))reason="视频平台请求失败，请检查账号授权、访问白名单或平台状态";}
    else if(failure instanceof java.io.IOException)reason="视频平台网络连接失败";
    else if(failure instanceof org.springframework.dao.DataAccessException)reason="数据库写入失败，请检查数据库连接";
    db.update("UPDATE fire_video_sync_state SET state='failed',message=?,finished_at=NOW() WHERE source_id=? AND job_id=?",text(reason,350)+"；原台账及坐标已保留",source,job);
   }
  }));}catch(RejectedExecutionException stopped){db.update("UPDATE fire_video_sync_state SET state='failed',message='服务正在关闭，请重试' WHERE source_id=? AND job_id=?",source,job);throw new ServiceException("服务正在关闭，请重试");}
  return Map.of("jobId",job,"state","running");
 }
 public void applySnapshot(String source,Snapshot snapshot,String job){
  // The whole snapshot is committed only after traversal and every cursor completed.
  tx.executeWithoutResult(status->{
   var state=db.queryForList("SELECT job_id FROM fire_video_sync_state WHERE source_id=? FOR UPDATE",source);if(state.isEmpty()||!Objects.equals(job,state.get(0).get("job_id")))throw new ServiceException("同步任务已失效");
   for(var d:snapshot.directories)db.update("INSERT INTO fire_video_directory(source_id,directory_id,parent_id,name,directory_path,directory_chain,last_seen_at) VALUES(?,?,?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),directory_path=VALUES(directory_path),directory_chain=VALUES(directory_chain),last_seen_at=NOW()",source,d.id,d.parentId,d.name,d.path,d.chain);
   // Match coordinate commits' camera-id lock order when a survey import overlaps sync.
   var ids=new HashMap<String,Long>();for(var row:db.queryForList("SELECT device_code,camera_id FROM fire_video_camera WHERE source_id=?",source))ids.put(String.valueOf(row.get("device_code")),((Number)row.get("camera_id")).longValue());
   var ordered=snapshot.devices.stream().sorted(Comparator.comparingLong((Device d)->ids.getOrDefault(d.code,Long.MAX_VALUE)).thenComparing(Device::code)).toList();
   for(var d:ordered)db.update("INSERT INTO fire_video_camera(source_id,device_code,device_name,directory_id,directory_path,directory_chain,platform_status,last_seen_at) VALUES(?,?,?,?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE device_name=VALUES(device_name),directory_id=VALUES(directory_id),directory_path=VALUES(directory_path),directory_chain=VALUES(directory_chain),platform_status=VALUES(platform_status),last_seen_at=NOW()",source,d.code,d.name,d.directoryId,d.path,d.chain,d.status);
   db.update("UPDATE fire_video_sync_state SET state='complete',directories=?,devices=?,message='完整同步成功；坐标已保留',last_success_at=NOW(),finished_at=NOW() WHERE source_id=? AND job_id=?",snapshot.directories.size(),snapshot.devices.size(),source,job);
  });
 }
 public List<Map<String,Object>> directories(){return db.queryForList("SELECT directory_id AS id,parent_id AS parentId,name,directory_path AS path FROM fire_video_directory WHERE source_id=? ORDER BY directory_path,directory_id",client.sourceId());}
 private record Where(String sql,List<Object> args){}
 private Where where(String source,String keyword,String directory,String location){
  String sql=" WHERE source_id=?";var args=new ArrayList<Object>();args.add(source);
  if(keyword!=null&&!keyword.isBlank()){if(keyword.length()>128)throw new ServiceException("搜索词最多128字");sql+=" AND (LOCATE(?,device_name)>0 OR LOCATE(?,device_code)>0 OR LOCATE(?,installation_location)>0)";for(int i=0;i<3;i++)args.add(keyword.trim());}
  if(directory!=null&&!directory.isBlank()){if(!directory.matches("\\d{1,18}"))throw new ServiceException("目录编码无效");sql+=" AND LOCATE(?,directory_chain)>0";args.add("/"+directory+"/");}
  if("pending".equals(location))sql+=" AND (longitude IS NULL OR latitude IS NULL)";
  else if("located".equals(location))sql+=" AND longitude IS NOT NULL AND latitude IS NOT NULL";
  else if("review".equals(location)||"confirmed".equals(location)){sql+=" AND location_status=?";args.add(location);}
  else if(location!=null&&!location.isBlank())throw new ServiceException("定位状态无效");return new Where(sql,args);
 }
 public Map<String,Object> list(String keyword,String directory,String location,int page,int size){
  String source=client.sourceId();Where w=where(source,keyword,directory,location);page=Math.max(1,Math.min(50000,page));size=Math.max(1,Math.min(1000,size));
  long total=db.queryForObject("SELECT COUNT(*) FROM fire_video_camera"+w.sql,Long.class,w.args.toArray());var args=new ArrayList<Object>(w.args);args.add(size);args.add((page-1)*size);
  var out=new LinkedHashMap<String,Object>();out.put("rows",db.queryForList(SELECT+w.sql+" ORDER BY camera_id LIMIT ? OFFSET ?",args.toArray()));out.put("total",total);
  out.put("summary",db.queryForMap("SELECT COUNT(*) AS total,COALESCE(SUM(longitude IS NOT NULL AND latitude IS NOT NULL),0) AS located,COALESCE(SUM(longitude IS NULL OR latitude IS NULL),0) AS pending FROM fire_video_camera WHERE source_id=?",source));
  var states=db.queryForList("SELECT CASE WHEN state='running' AND started_at<DATE_SUB(NOW(),INTERVAL 15 MINUTE) THEN 'failed' ELSE state END AS state,CASE WHEN state='running' AND started_at<DATE_SUB(NOW(),INTERVAL 15 MINUTE) THEN '上次同步已中断或超时，可重新同步；原台账已保留' ELSE message END AS message,directories,devices,started_at AS startedAt,finished_at AS finishedAt,last_success_at AS lastSuccessAt FROM fire_video_sync_state WHERE source_id=?",source);out.put("sync",states.isEmpty()?Map.of("state","idle","message","尚未同步平台目录"):states.get(0));return out;
 }
 public List<Map<String,Object>> export(String keyword,String directory,String location){Where w=where(client.sourceId(),keyword,directory,location);var rows=db.queryForList(SELECT+w.sql+" ORDER BY camera_id LIMIT "+(MAX_ROWS+1),w.args.toArray());if(rows.size()>MAX_ROWS)throw new ServiceException("导出超过50000条，请按目录分批导出");return rows;}
 public Map<String,Object> map(){var rows=db.queryForList(SELECT+" WHERE source_id=? AND longitude BETWEEN -180 AND 180 AND latitude BETWEEN -90 AND 90 AND NOT(longitude=0 AND latitude=0) ORDER BY camera_id LIMIT 5001",client.sourceId());return Map.of("rows",rows.stream().limit(5000).toList(),"truncated",rows.size()>5000);}
 public Map<String,Object> get(long id){var rows=db.queryForList(SELECT+" WHERE source_id=? AND camera_id=?",client.sourceId(),id);if(rows.isEmpty())throw new ServiceException("监控不存在或无权访问");return rows.get(0);}
 private static boolean same(Object a,BigDecimal b){return a==null?b==null:b!=null&&new BigDecimal(String.valueOf(a)).compareTo(b)==0;}
 private void classify(List<CoordinateRow> rows,String source,String mode){
  for(var row:rows){if(!"ready".equals(row.status))continue;
   if(!source.equals(row.source)){row.status="invalid";row.reason="视频来源不属于当前账号";continue;}
   var found=db.queryForList(SELECT+" WHERE source_id=? AND device_code=?",source,row.deviceCode);if(found.isEmpty()){row.status="invalid";row.reason="设备不在当前台账，请先同步监控目录";continue;}
   var old=found.get(0);row.cameraId=Long.parseLong(String.valueOf(old.get("id")));row.name=String.valueOf(old.get("name"));row.oldLongitude=(BigDecimal)old.get("lng");row.oldLatitude=(BigDecimal)old.get("lat");
   if(row.revision!=((Number)old.get("revision")).intValue()){row.status="conflict";row.reason="坐标已被修改，请重新导出后填写";continue;}
   if(!"overwrite".equals(mode)&&old.get("lng")!=null&&old.get("lat")!=null){row.status="skipped";row.reason="仅补空坐标，已有位置保持不变";continue;}
   if(row.installationLocation.isBlank())row.installationLocation=String.valueOf(old.get("installationLocation"));if(row.remark.isBlank())row.remark=String.valueOf(old.get("remark"));
   if(!row.manualVerification&&same(old.get("lng"),row.longitude)&&same(old.get("lat"),row.latitude))row.locationStatus=String.valueOf(old.get("locationStatus"));
   if(same(old.get("lng"),row.longitude)&&same(old.get("lat"),row.latitude)&&Objects.equals(old.get("installationLocation"),row.installationLocation)&&Objects.equals(old.get("remark"),row.remark)&&Objects.equals(old.get("locationStatus"),row.locationStatus)){row.status="unchanged";row.reason="位置及备注没有变化";continue;}
   row.status="update";row.reason=old.get("lng")==null?"将补充坐标，定位待核验":"将更新位置，请核对前后差异";
  }
 }
 public Map<String,Object> preview(byte[] bytes,String filename,String mode,long owner)throws Exception{
  String source=client.sourceId();if(!Set.of("fill","overwrite").contains(mode))throw new ServiceException("导入模式无效");var rows=FireVideoCoordinateExcel.read(bytes,filename);classify(rows,source,mode);return savePreview(rows,filename,mode,source,owner);
 }
 private Map<String,Object> savePreview(List<CoordinateRow> rows,String filename,String mode,String source,long owner){return tx.execute(status->{String id=UUID.randomUUID().toString();db.update("INSERT INTO fire_video_import_batch(batch_id,source_id,owner_id,file_name,mode,expires_at) VALUES(?,?,?,?,?,?)",id,source,owner,filename,mode,Timestamp.valueOf(LocalDateTime.now().plusHours(2)));for(var r:rows)db.update("INSERT INTO fire_video_import_row(batch_id,excel_row,payload) VALUES(?,?,?)",id,r.rowNumber,json.writeValueAsString(r));return response(id,"ready",rows);});}
 private Map<String,Object> response(String id,String state,List<CoordinateRow> rows){var counts=new LinkedHashMap<String,Object>();counts.put("total",rows.size());for(String s:List.of("update","updated","skipped","invalid","conflict","unchanged"))counts.put(s,rows.stream().filter(r->s.equals(r.status)).count());return Map.of("batchId",id,"state",state,"summary",counts,"rows",rows);}
 public Map<String,Object> commit(String id,long owner,String actor){
  String source=client.sourceId();if(id==null||!id.matches("[0-9a-f-]{36}"))throw new ServiceException("导入批次无效");
  return tx.execute(status->{
   var batches=db.queryForList("SELECT * FROM fire_video_import_batch WHERE batch_id=? AND owner_id=? AND source_id=? FOR UPDATE",id,owner,source);if(batches.isEmpty())throw new ServiceException("导入批次不存在或不属于当前用户");var batch=batches.get(0);
   if("complete".equals(batch.get("state")))return json.readValue(String.valueOf(batch.get("result_json")),Map.class);
   Object date=batch.get("expires_at");LocalDateTime expiry=date instanceof LocalDateTime t?t:((Timestamp)date).toLocalDateTime();if(expiry.isBefore(LocalDateTime.now()))throw new ServiceException("预检已过期，请重新上传");
   var rows=new ArrayList<CoordinateRow>();for(var raw:db.queryForList("SELECT payload FROM fire_video_import_row WHERE batch_id=? ORDER BY excel_row",id))rows.add(json.readValue(String.valueOf(raw.get("payload")),CoordinateRow.class));
   for(var row:rows.stream().filter(r->"update".equals(r.status)).sorted(Comparator.comparingLong(r->r.cameraId)).toList()){
    var found=db.queryForList("SELECT * FROM fire_video_camera WHERE camera_id=? AND source_id=? AND device_code=? FOR UPDATE",row.cameraId,source,row.deviceCode);if(found.isEmpty())throw new ServiceException("设备已变化，请重新预检");var before=found.get(0);
    if(row.revision!=((Number)before.get("coordinate_revision")).intValue()||("fill".equals(batch.get("mode"))&&before.get("longitude")!=null&&before.get("latitude")!=null))throw new ServiceException("坐标版本冲突，整批未写入，请重新预检");
    db.update("UPDATE fire_video_camera SET source_longitude=?,source_latitude=?,source_coord_system=?,longitude=?,latitude=?,location_status=?,installation_location=?,remark=?,coordinate_revision=coordinate_revision+1,coordinate_by=?,coordinate_time=NOW() WHERE camera_id=? AND source_id=?",row.sourceLongitude,row.sourceLatitude,row.crs,row.longitude,row.latitude,row.locationStatus,row.installationLocation,row.remark,actor,row.cameraId,source);
    var after=db.queryForMap("SELECT * FROM fire_video_camera WHERE camera_id=?",row.cameraId);row.status="updated";row.reason="已写入数据库";
    db.update("UPDATE fire_video_import_row SET payload=?,before_json=?,after_json=? WHERE batch_id=? AND excel_row=?",json.writeValueAsString(row),json.writeValueAsString(before),json.writeValueAsString(after),id,row.rowNumber);
   }
   var result=response(id,"complete",rows);db.update("UPDATE fire_video_import_batch SET state='complete',result_json=?,finished_at=NOW() WHERE batch_id=?",json.writeValueAsString(result),id);return result;
  });
 }
 public Map<String,Object> edit(long id,CoordinateRow row,long owner,String actor){
  var old=get(id);row.cameraId=id;row.source=String.valueOf(old.get("source"));row.deviceCode=String.valueOf(old.get("deviceCode"));row.rowNumber=1;
  if(row.installationLocation==null||row.remark==null||row.installationLocation.length()>500||row.remark.length()>1000)throw new ServiceException("安装位置最多500字，备注最多1000字");
  if(row.locationStatus==null||!Set.of("review","confirmed").contains(row.locationStatus))throw new ServiceException("定位状态无效");row.crs=FireVideoCoordinates.system(row.crs);var mapped=FireVideoCoordinates.convert(row.sourceLongitude,row.sourceLatitude,row.crs);row.longitude=mapped[0];row.latitude=mapped[1];row.status="ready";row.manualVerification=true;
  var rows=new ArrayList<>(List.of(row));classify(rows,row.source,"overwrite");if("conflict".equals(row.status))throw new ServiceException(row.reason);var preview=savePreview(rows,"单条坐标维护","overwrite",row.source,owner);return commit(String.valueOf(preview.get("batchId")),owner,actor);
 }
}
