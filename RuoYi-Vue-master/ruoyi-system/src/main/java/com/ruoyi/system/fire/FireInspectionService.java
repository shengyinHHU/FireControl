package com.ruoyi.system.fire;
import java.util.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.service.*;
@Service
public class FireInspectionService {
 private final JdbcTemplate db;private final FireInspectionReadService reads;private final FireVideoClient video;private final IFirePlaceService places;private final Path evidenceRoot;
 public FireInspectionService(DataSource source,FireInspectionReadService read,FireVideoClient client,IFirePlaceService place,@org.springframework.beans.factory.annotation.Value("${fire.inspection.evidence-path:}") String path){
  db=new JdbcTemplate(source);reads=read;video=client;places=place;
  Path profile=Path.of(RuoYiConfig.getProfile()==null?"uploadPath":RuoYiConfig.getProfile()).toAbsolutePath().normalize();
  evidenceRoot=(path.isBlank()?profile.resolveSibling(profile.getFileName()+"-inspection-private"):Path.of(path)).toAbsolutePath().normalize();
  if(evidenceRoot.startsWith(profile))throw new IllegalArgumentException("查岗证据目录必须位于公开profile目录之外");
 }
 public Map<String,Object> post(long id){var q=new FireInspectionQuery();q.setId(id);var rows=reads.posts(q);if(rows.isEmpty())throw new ServiceException("岗位不存在或无权访问",404);var row=new LinkedHashMap<>(rows.get(0));row.put("cameras",cameras(id));return row;}
 public List<Map<String,Object>> cameras(long id){return db.queryForList("SELECT CAST(binding_id AS CHAR) bindingId,device_code deviceCode,device_name deviceName,is_primary isPrimary,sort_order sortOrder FROM fire_inspection_post_camera WHERE post_id=? AND status='0' ORDER BY is_primary DESC,sort_order,binding_id",id);}
 public Map<String,Object> record(long id){var q=new FireInspectionQuery();q.setId(id);var rows=reads.records(q);if(rows.isEmpty())throw new ServiceException("记录不存在或无权访问",404);return rows.get(0);}
 private Map<String,Object> lock(long id){var row=post(id);db.queryForList("SELECT post_id FROM fire_inspection_post WHERE post_id=? FOR UPDATE",id);return post(id);}
 private Map<String,Object> activeBinding(long id,long binding){var p=post(id);if(!"0".equals(p.get("status")))throw new ServiceException("岗位已停用，不能发起新的查岗");return cameras(id).stream().filter(c->String.valueOf(c.get("bindingId")).equals(String.valueOf(binding))).findFirst().orElseThrow(()->new ServiceException("摄像头已解绑或不属于该岗位"));}
 public Object preview(long id,long binding,String protocol)throws Exception {var camera=activeBinding(id,binding);String code=String.valueOf(camera.get("deviceCode"));video.resolveDevice(code);return video.preview(code,protocol);}
 public Map<String,Object> verifyCamera(String code)throws Exception {return video.resolveDevice(code);}
 @Transactional(rollbackFor=Exception.class)
 public Map<String,Object> savePost(Long id,Map<String,Object> input)throws Exception {
  String name=text(input,"postName",160,true),remark=text(input,"remark",500,false),status=text(input,"status",1,true);if(!Set.of("0","1").contains(status))throw new ServiceException("岗位状态无效");
  String contactName=text(input,"contactName",100,false),contactPhone=text(input,"contactPhone",64,false);
  int sort=integer(input.get("sortOrder"),0);if(sort<0||sort>100000)throw new ServiceException("排序超出范围");
  Long place=optionalId(input.get("placeId"));
  if(place!=null){var q=new FirePlace();q.setPlaceId(place);if(places.selectMapPlaces(q).isEmpty())throw new ServiceException("所属机构不存在或无权访问");}
  if(!(input.get("cameras") instanceof List<?> list)||list.isEmpty()||list.size()>12)throw new ServiceException("每个岗位请选择1至12台摄像头");
  var cameras=new ArrayList<Map<String,Object>>();var codes=new HashSet<String>();int primary=0;
  for(Object value:list){if(!(value instanceof Map<?,?> raw))throw new ServiceException("摄像头配置无效");String code=String.valueOf(raw.get("deviceCode"));if(!codes.add(code))throw new ServiceException("同一岗位不能重复绑定摄像头");var device=video.resolveDevice(code);boolean main=Boolean.TRUE.equals(raw.get("isPrimary"))||"1".equals(String.valueOf(raw.get("isPrimary")));if(main)primary++;String deviceName=String.valueOf(device.getOrDefault("deviceName",code));if(deviceName.length()>255)throw new ServiceException("设备名称过长");cameras.add(Map.of("code",code,"name",deviceName,"primary",main?1:0));}
  if(primary!=1)throw new ServiceException("请选择且仅选择一台默认摄像头");
  String username=SecurityUtils.getUsername();
  if(id==null){var key=new GeneratedKeyHolder();db.update(conn->{var p=conn.prepareStatement("INSERT INTO fire_inspection_post(post_name,place_id,dept_id,supervisor_user_id,status,sort_order,remark,create_by,create_time,update_by,update_time,contact_name,contact_phone) VALUES(?,?,?,?,?,?,?,?,NOW(),?,NOW(),?,?)",Statement.RETURN_GENERATED_KEYS);p.setObject(1,name);p.setObject(2,place);p.setObject(3,SecurityUtils.getDeptId());p.setObject(4,SecurityUtils.getUserId());p.setObject(5,status);p.setObject(6,sort);p.setObject(7,remark);p.setObject(8,username);p.setObject(9,username);p.setObject(10,contactName);p.setObject(11,contactPhone);return p;},key);id=key.getKey().longValue();}
  else {var old=lock(id);if(integer(input.get("revision"),-1)!=((Number)old.get("revision")).intValue())throw new ServiceException("岗位已被其他人修改，请刷新后重试");db.update("UPDATE fire_inspection_post SET post_name=?,place_id=?,status=?,sort_order=?,remark=?,contact_name=?,contact_phone=?,revision=revision+1,update_by=?,update_time=NOW() WHERE post_id=?",name,place,status,sort,remark,contactName,contactPhone,username,id);}
  db.update("UPDATE fire_inspection_post_camera SET status='1',is_primary=0,update_time=NOW() WHERE post_id=?",id);
  int index=0;for(var camera:cameras)db.update("INSERT INTO fire_inspection_post_camera(post_id,video_source,device_code,device_name,is_primary,sort_order,status,create_time,update_time) VALUES(?,'ctseelink-default',?,?,?,?,'0',NOW(),NOW()) ON DUPLICATE KEY UPDATE device_name=VALUES(device_name),is_primary=VALUES(is_primary),sort_order=VALUES(sort_order),status='0',update_time=NOW()",id,camera.get("code"),camera.get("name"),camera.get("primary"),index++);
  return post(id);
 }
 @Transactional(rollbackFor=Exception.class)
 public Map<String,Object> saveRecord(Map<String,Object> input,byte[] image)throws Exception {
  long postId=id(input.get("postId")),bindingId=id(input.get("bindingId"));String request=text(input,"requestId",36,true),result=text(input,"result",16,true),note=text(input,"note",1000,false);
  if(!request.matches("[0-9a-fA-F-]{36}")||!Set.of("present","absent","unknown").contains(result))throw new ServiceException("查岗提交参数无效");
  byte[] evidence=normalizeImage(image);String imageHash=evidence==null?"":hash(evidence);String content=hash((postId+"\n"+bindingId+"\n"+result+"\n"+note+"\n"+imageHash).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var previous=existing(request,content);if(previous!=null)return previous;
  var post=lock(postId);var camera=activeBinding(postId,bindingId);previous=existing(request,content);if(previous!=null)return previous;
  var scope=db.queryForMap("SELECT dept_id,supervisor_user_id FROM fire_inspection_post WHERE post_id=?",postId);String filename=null;
  if(evidence!=null){Files.createDirectories(evidenceRoot);filename=UUID.randomUUID()+".jpg";Path path=evidenceRoot.resolve(filename);Files.write(path,evidence,StandardOpenOption.CREATE_NEW);
   if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(path);}catch(Exception ignored){}}});
  }
  final String saved=filename;var key=new GeneratedKeyHolder();
  try{db.update(conn->{var p=conn.prepareStatement("INSERT INTO fire_inspection_record(request_id,post_id,binding_id,place_id,post_name,place_name,video_source,device_code,device_name,inspector_user_id,inspector_name,dept_id,supervisor_user_id,result,note,evidence_file,evidence_sha256,evidence_size,content_hash,inspect_time) VALUES(?,?,?,?,?,?,'ctseelink-default',?,?,?,?,?,?,?,?,?,?,?,?,NOW())",Statement.RETURN_GENERATED_KEYS);
   Object[] values={request,postId,bindingId,post.get("placeId"),post.get("postName"),post.get("placeName"),camera.get("deviceCode"),camera.get("deviceName"),SecurityUtils.getUserId(),SecurityUtils.getLoginUser().getUser().getNickName(),scope.get("dept_id"),scope.get("supervisor_user_id"),result,note,saved,evidence==null?null:imageHash,evidence==null?null:evidence.length,content};for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);return p;},key);}
  catch(Exception failure){if(saved!=null)Files.deleteIfExists(evidenceRoot.resolve(saved));throw failure;}
  return record(key.getKey().longValue());
 }
 private Map<String,Object> existing(String request,String content){var rows=db.queryForList("SELECT record_id,inspector_user_id,content_hash FROM fire_inspection_record WHERE request_id=?",request);if(rows.isEmpty())return null;var row=rows.get(0);if(((Number)row.get("inspector_user_id")).longValue()!=SecurityUtils.getUserId()||!content.equals(row.get("content_hash")))throw new ServiceException("提交标识已使用，请重新提交");return record(((Number)row.get("record_id")).longValue());}
 public byte[] evidence(long id)throws Exception {record(id);String file=db.queryForObject("SELECT evidence_file FROM fire_inspection_record WHERE record_id=?",String.class,id);if(file==null)throw new ServiceException("该记录没有抓拍证据",404);if(!file.matches("[0-9a-f-]{36}\\.jpg"))throw new ServiceException("证据文件标识异常");Path path=evidenceRoot.resolve(file);if(!Files.isRegularFile(path)||Files.size(path)>5*1024*1024)throw new ServiceException("证据文件不存在或异常",404);return Files.readAllBytes(path);}
 static byte[] normalizeImage(byte[] bytes)throws Exception {
  if(bytes==null||bytes.length==0)return null;if(bytes.length>5*1024*1024)throw new ServiceException("抓拍图片不能超过5MB");
  try(var stream=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))){var readers=javax.imageio.ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new ServiceException("抓拍文件不是有效图片");var reader=readers.next();try{reader.setInput(stream);String format=reader.getFormatName();if(!Set.of("JPEG","JPG","PNG").contains(format.toUpperCase(Locale.ROOT))||reader.getWidth(0)>4096||reader.getHeight(0)>4096)throw new ServiceException("仅支持4096像素以内的JPEG或PNG抓拍");var source=reader.read(0);var rgb=new java.awt.image.BufferedImage(source.getWidth(),source.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=rgb.createGraphics();try{g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,rgb.getWidth(),rgb.getHeight());g.drawImage(source,0,0,null);}finally{g.dispose();}var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(rgb,"jpg",out);if(out.size()>5*1024*1024)throw new ServiceException("处理后的抓拍图片过大");return out.toByteArray();}finally{reader.dispose();}}
 }
 static String hash(byte[] value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}
 public static long id(Object value){Long parsed=optionalId(value);if(parsed==null)throw new ServiceException("缺少有效标识");return parsed;}
 static Long optionalId(Object value){if(value==null||String.valueOf(value).isBlank())return null;try{long id=Long.parseLong(String.valueOf(value));if(id>0)return id;}catch(Exception ignored){}throw new ServiceException("标识无效");}
 static int integer(Object value,int fallback){if(value==null)return fallback;try{return Integer.parseInt(String.valueOf(value));}catch(Exception invalid){throw new ServiceException("整数参数无效");}}
 static String text(Map<String,Object> input,String key,int max,boolean required){Object raw=input.get(key);String value=raw==null?"":String.valueOf(raw).trim();if(value.length()>max||(required&&value.isBlank()))throw new ServiceException(key+"为空或超出长度限制");return value;}
}
