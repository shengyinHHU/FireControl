import java.io.*;
import java.nio.file.*;
import java.math.*;
import java.util.*;
import com.ruoyi.system.fire.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import tools.jackson.databind.ObjectMapper;

public class VideoCatalogIntegrationTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 static void login(long id){var user=new LoginUser();user.setUserId(id);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
 static void denied(Runnable r,String reason){try{r.run();throw new AssertionError(reason);}catch(ServiceException expected){}}
 static class Vendor extends FireVideoClient {
  boolean broken=false;String label="同名监控";
  Vendor(FireVideoProperties p){super(p,new ObjectMapper(),new FireVideoMedia());}
  public Object regions(Long region){requireAccess();return region==null?List.of(Map.of("id",1,"name","主目录")):region==1?List.of(Map.of("id",2,"name","子目录")):List.of();}
  public Object devices(Long region,long last,int size){requireAccess();if(region==null)return Map.of("list",last<0?List.of(Map.of("deviceCode","000012345678901234","deviceName",label),Map.of("deviceCode","code-2","deviceName",label)):List.of(Map.of("deviceCode","code-3","deviceName","第三个监控")),"lastId",broken?22:last<0?22:-1);return Map.of("list",region==2?List.of(Map.of("deviceCode","000012345678901234","deviceName",label)):List.of(),"lastId",-1);}
 }
 static byte[] workbook(FireVideoCatalogService service,boolean mutate)throws Exception{
  var bytes=new ByteArrayOutputStream();FireVideoCoordinateExcel.write(service.export(null,null,null),bytes);
  if(!mutate)return bytes.toByteArray();try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))){var s=book.getSheetAt(0);check(s.getRow(1).getCell(1).getCellType()==CellType.STRING,"code must be text");check(s.getRow(1).getCell(1).getStringCellValue().equals("000012345678901234"),"leading zero preserved");check(s.getRow(1).getCell(5).getCellType()==CellType.BLANK,"coordinates initially blank");
   for(int i=1;i<=2;i++){s.getRow(i).getCell(5).setCellValue("118.75");s.getRow(i).getCell(6).setCellValue("32.23");s.getRow(i).getCell(7).setCellValue(i==1?"GCJ-02":"WGS84");}
   var out=new ByteArrayOutputStream();book.write(out);return out.toByteArray();}
 }
 static byte[] alter(byte[] bytes,java.util.function.Consumer<XSSFWorkbook> action)throws Exception{try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes))){action.accept(book);var out=new ByteArrayOutputStream();book.write(out);return out.toByteArray();}}
 static long count(Map response,String status){return ((Number)((Map)response.get("summary")).get(status)).longValue();}
 public static void main(String[] args)throws Exception{
  var ds=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8","root","");var db=new JdbcTemplate(ds);
  var config=new FireVideoProperties();config.setEnterpriseUser("fixture-account");config.setAllowedUserIds(List.of(2L));var vendor=new Vendor(config);login(1);
  var service=new FireVideoCatalogService(ds,vendor,new ObjectMapper(),new DataSourceTransactionManager(ds));
  try{
   service.startSync();for(int i=0;i<100;i++){if(!"running".equals(db.queryForObject("SELECT state FROM fire_video_sync_state",String.class)))break;Thread.sleep(50);}
   check(db.queryForObject("SELECT state FROM fire_video_sync_state",String.class).equals("complete"),"async authenticated sync");
   check(db.queryForObject("SELECT COUNT(*) FROM fire_video_camera",Integer.class)==3,"all cursors and dedup");check(db.queryForObject("SELECT COUNT(*) FROM fire_video_directory",Integer.class)==2,"nested directories");
   check(((Number)service.list(null,"1",null,1,20).get("total")).intValue()==1,"descendant filtering");
   check(((List)service.map().get("rows")).isEmpty(),"unlocated devices excluded");
   byte[] filled=workbook(service,true);var preview=service.preview(filled,"调查.xlsx","fill",1);check(count(preview,"update")==2&&count(preview,"skipped")==1,"preview classification");String batch=(String)preview.get("batchId");
   login(2);denied(()->service.commit(batch,2,"user2"),"foreign owner accepted");login(1);
   var committed=service.commit(batch,1,"admin");check(count(committed,"updated")==2,"commit updates");service.commit(batch,1,"admin");
   check(db.queryForObject("SELECT SUM(coordinate_revision) FROM fire_video_camera",Integer.class)==2,"idempotent retry");
   check(db.queryForObject("SELECT COUNT(*) FROM fire_video_import_row WHERE before_json IS NOT NULL AND after_json IS NOT NULL",Integer.class)==2,"audit before and after");
   check(((List)service.map().get("rows")).size()==2,"valid database map points");
   BigDecimal gps=db.queryForObject("SELECT longitude FROM fire_video_camera WHERE device_code='code-2'",BigDecimal.class);check(gps.compareTo(new BigDecimal("118.75"))!=0,"GPS conversion");
   String source=vendor.sourceId();var snapshot=FireVideoCatalogService.collect(vendor,(d,c)->{});String job=db.queryForObject("SELECT job_id FROM fire_video_sync_state",String.class);vendor.label="重命名";service.applySnapshot(source,snapshot,job);check(db.queryForObject("SELECT SUM(coordinate_revision) FROM fire_video_camera",Integer.class)==2,"sync preserves coordinates");
   var reexport=workbook(service,false);var unchanged=service.preview(reexport,"原表.xlsx","overwrite",1);check(count(unchanged,"unchanged")==2,"unchanged round trip");
   var newFile=alter(reexport,b->{b.getSheetAt(0).getRow(1).getCell(5).setCellValue("118.751");b.getSheetAt(0).getRow(2).getCell(5).setCellValue("118.752");});var stale=service.preview(newFile,"改坐标.xlsx","overwrite",1);
   var row=new FireVideoCoordinateExcel.CoordinateRow();row.sourceLongitude=new BigDecimal("118.753");row.sourceLatitude=new BigDecimal("32.23");row.crs="GCJ02";row.revision=1;row.installationLocation="实地安装点";row.remark="调查";row.locationStatus="confirmed";
   service.edit(1,row,1,"admin");BigDecimal before=db.queryForObject("SELECT longitude FROM fire_video_camera WHERE device_code='code-2'",BigDecimal.class);denied(()->service.commit((String)stale.get("batchId"),1,"admin"),"stale batch accepted");check(before.compareTo(db.queryForObject("SELECT longitude FROM fire_video_camera WHERE device_code='code-2'",BigDecimal.class))==0,"conflict rolls back entire transaction");
   var confirmedRoundTrip=service.preview(workbook(service,false),"已核验.xlsx","overwrite",1);check(count(confirmedRoundTrip,"unchanged")==2,"round trip does not downgrade confirmed locations");
   db.update("UPDATE fire_video_sync_state SET state='running',started_at=DATE_SUB(NOW(),INTERVAL 20 MINUTE)");check("failed".equals(((Map)service.list(null,null,null,1,20).get("sync")).get("state")),"interrupted jobs can be retried from UI");
   var duplicate=alter(filled,b->b.getSheetAt(0).getRow(2).getCell(1).setCellValue("000012345678901234"));check(count(service.preview(duplicate,"重复.xlsx","fill",1),"invalid")==2,"all duplicate rows rejected");
   var unknown=alter(filled,b->b.getSheetAt(0).getRow(1).getCell(1).setCellValue("unknown-device"));check(count(service.preview(unknown,"未知.xlsx","overwrite",1),"invalid")>=1,"unknown devices rejected");
   var formula=alter(filled,b->b.getSheetAt(0).getRow(1).getCell(5).setCellFormula("1+1"));check(count(service.preview(formula,"公式.xlsx","fill",1),"invalid")>=1,"formulas rejected");
   var numeric=alter(filled,b->{var c=b.getSheetAt(0).getRow(1).getCell(1);c.setBlank();c.setCellValue(1234);});check(count(service.preview(numeric,"数字编码.xlsx","fill",1),"invalid")>=1,"numeric codes rejected");
   vendor.broken=true;try{FireVideoCatalogService.collect(vendor,(d,c)->{});throw new AssertionError("bad cursor accepted");}catch(ServiceException expected){}check(db.queryForObject("SELECT COUNT(*) FROM fire_video_camera",Integer.class)==3,"failed sync preserved catalog");
   config.setEnterpriseUser("other-account");check(((Number)service.list(null,null,null,1,20).get("total")).longValue()==0,"source/account isolation");config.setEnterpriseUser("fixture-account");login(3);denied(()->service.list(null,null,null,1,20),"unconfigured account access");login(1);
   var bd=FireVideoCoordinates.convert(new BigDecimal("118.7565"),new BigDecimal("32.236"),"BD09");check(bd[0].subtract(new BigDecimal("118.75")).abs().doubleValue()<.0002,"BD09 conversion");
   System.out.println("VIDEO_CATALOG_INTEGRATION_OK: full pagination, directory recursion, dedup, blank/text export, import conversion, owner/account isolation, idempotency, coordinate preservation, duplicate/formula rejection, optimistic conflicts and transactional audit");
  }finally{service.close();}
 }
}
