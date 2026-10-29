import java.util.*;
import java.nio.file.*;
import java.sql.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.core.io.FileSystemResource;
import org.mybatis.spring.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.ruoyi.framework.aspectj.DataScopeAspect;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.fire.*;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.*;
public class InspectionIntegrationCheck {
 static int checks;static JdbcTemplate db;
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
 interface Run{void go()throws Exception;}
 static void denied(Run action,String label)throws Exception {try{action.go();throw new AssertionError(label);}catch(ServiceException expected){checks++;}}
 static void login(long id,long dept,String scope){var role=new SysRole();role.setRoleId(2L);role.setStatus("0");role.setDataScope(scope);role.setPermissions(Set.of("fire:inspections:list"));var user=new SysUser();user.setUserId(id);user.setDeptId(dept);user.setUserName("user"+id);user.setNickName("测试员"+id);user.setRoles(List.of(role));var login=new LoginUser();login.setUser(user);login.setUserId(id);login.setDeptId(dept);login.setPermissions(Set.of("fire:inspections:list","fire:inspections:edit","fire:inspections:add"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));}
 static Map<String,Object> postInput(String name,int revision,String status,String...codes){var m=new LinkedHashMap<String,Object>();m.put("postName",name);m.put("placeId",1);m.put("status",status);m.put("sortOrder",0);m.put("remark","");m.put("revision",revision);m.put("contactName","张三");m.put("contactPhone","025-12345678 转 801");var cameras=new ArrayList<>();for(int i=0;i<codes.length;i++)cameras.add(Map.of("deviceCode",codes[i],"deviceName","伪造名称","isPrimary",i==0));m.put("cameras",cameras);return m;}
 static byte[] image()throws Exception {var img=new java.awt.image.BufferedImage(20,10,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=img.createGraphics();g.setColor(java.awt.Color.CYAN);g.fillRect(0,0,20,10);g.dispose();var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(img,"png",out);return out.toByteArray();}
 public static void main(String[] args)throws Exception {
  var requestState=new HashMap<String,Object>();org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.RequestAttributes(){
   public Object getAttribute(String n,int scope){return requestState.get(n);}public void setAttribute(String n,Object v,int scope){requestState.put(n,v);}public void removeAttribute(String n,int scope){requestState.remove(n);}public String[] getAttributeNames(int scope){return requestState.keySet().toArray(String[]::new);}public void registerDestructionCallback(String n,Runnable r,int scope){}public Object resolveReference(String key){return null;}public String getSessionId(){return "fixture";}public Object getSessionMutex(){return this;}
  });
  String url="jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";var ds=new DriverManagerDataSource(url,"root","");db=new JdbcTemplate(ds);Path root=Path.of(args[1]),work=Path.of(args[2]);
  var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);factory.setMapperLocations(new FileSystemResource(root.resolve("RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FireInspectionMapper.xml")));var session=new SqlSessionTemplate(factory.getObject());var mapper=session.getMapper(FireInspectionMapper.class);
  var scoped=new AspectJProxyFactory(new FireInspectionReadService(mapper));scoped.addAspect(new DataScopeAspect());FireInspectionReadService reads=scoped.getProxy();
  var video=new FireVideoClient(new FireVideoProperties(),new tools.jackson.databind.ObjectMapper(),new FireVideoMedia()){
   public Map<String,Object> resolveDevice(String code){if(!Set.of("A","B").contains(code))throw new ServiceException("设备不属于账号");return Map.of("deviceCode",code,"deviceName","官方名称"+code);}
   public Object preview(String code,String protocol){return Map.of("url","/fire/video/flv/fixture","protocol",protocol);}
  };
  var place=(IFirePlaceService)java.lang.reflect.Proxy.newProxyInstance(IFirePlaceService.class.getClassLoader(),new Class[]{IFirePlaceService.class},(p,m,a)->{var q=(FirePlace)a[0];if(q.getPlaceId()!=null&&q.getPlaceId()==1L)return List.of(new FirePlace());return List.of();});
  var config=new RuoYiConfig();config.setProfile(work.resolve("public").toString());Path evidence=work.resolve("private");
  var target=new FireInspectionService(ds,reads,video,place,evidence.toString());var tx=new TransactionProxyFactoryBean();tx.setTarget(target);tx.setProxyTargetClass(true);tx.setTransactionManager(new DataSourceTransactionManager(ds));var attrs=new Properties();attrs.setProperty("save*","PROPAGATION_REQUIRED,-java.lang.Exception");tx.setTransactionAttributes(attrs);tx.afterPropertiesSet();var service=(FireInspectionService)tx.getObject();
  login(2,10,"5");var post=service.savePost(null,postInput("值守岗",0,"0","A","B"));long postId=Long.parseLong((String)post.get("postId"));var cameras=(List<Map<String,Object>>)post.get("cameras");long binding=Long.parseLong((String)cameras.get(0).get("bindingId"));check(cameras.size()==2,"multi-camera binding");check(cameras.get(0).get("deviceName").equals("官方名称A"),"vendor owns device name");check(reads.posts(new FireInspectionQuery()).size()==1,"owner sees post");
  check("张三".equals(post.get("contactName")),"contact name round-trip");check("025-12345678 转 801".equals(post.get("contactPhone")),"landline extension preserved");
  var tooLong=postInput("超长联系人",0,"0","A");tooLong.put("contactName","人".repeat(101));denied(()->service.savePost(null,tooLong),"contact length rejected");
  denied(()->service.savePost(null,postInput("伪造设备",0,"0","FAKE")),"unknown camera denied");check(db.queryForObject("SELECT COUNT(*) FROM fire_inspection_post",Integer.class)==1,"failed validation inserts nothing");
  login(3,20,"5");var attack=new FireInspectionQuery();attack.getParams().put("dataScope"," OR 1=1");check(reads.posts(attack).isEmpty(),"AOP clears injected scope");denied(()->service.post(postId),"foreign post detail denied");denied(()->service.savePost(postId,postInput("越权",1,"0","B")),"foreign update denied");
  login(4,10,"3");check(reads.posts(new FireInspectionQuery()).size()==1,"department-scoped read");login(2,10,"5");
  var input=new LinkedHashMap<String,Object>();input.put("postId",postId);input.put("bindingId",binding);input.put("requestId",UUID.randomUUID().toString());input.put("result","present");input.put("note","现场人工检查");input.put("inspectorName","冒充管理员");byte[] picture=image();
  var record=service.saveRecord(input,picture);long recordId=Long.parseLong((String)record.get("recordId"));check(record.get("inspectorName").equals("测试员2"),"server identity cannot be spoofed");check(record.get("inspectTime")!=null,"server timestamp");check(service.evidence(recordId).length>0,"authorized private evidence");check(Files.list(evidence).count()==1,"one durable file");
  check(service.saveRecord(input,picture).get("recordId").equals(record.get("recordId")),"same request returns original result");check(db.queryForObject("SELECT COUNT(*) FROM fire_inspection_record",Integer.class)==1,"idempotent record count");var altered=new LinkedHashMap<>(input);altered.put("note","changed");denied(()->service.saveRecord(altered,picture),"same key changed payload rejected");
  login(3,20,"5");denied(()->service.record(recordId),"foreign record denied");denied(()->service.evidence(recordId),"foreign evidence denied");login(2,10,"5");
  var changed=service.savePost(postId,postInput("岗位已改名",1,"0","B"));check("张三".equals(changed.get("contactName")),"updated contact round-trip");check(service.record(recordId).get("postName").equals("值守岗"),"historical post name immutable");check(service.record(recordId).get("deviceName").equals("官方名称A"),"historical camera immutable");denied(()->service.savePost(postId,postInput("旧编辑覆盖",1,"0","A")),"optimistic revision enforced");
  var stale=new LinkedHashMap<>(input);stale.put("requestId",UUID.randomUUID().toString());denied(()->service.saveRecord(stale,null),"unbound camera cannot write new records");
  long bindingB=Long.parseLong((String)((List<Map<String,Object>>)changed.get("cameras")).get(0).get("bindingId"));service.savePost(postId,postInput("停用岗",2,"1","B"));denied(()->service.preview(postId,bindingB,"flv"),"disabled post cannot preview");var unavailable=new LinkedHashMap<>(stale);unavailable.put("bindingId",bindingB);unavailable.put("result","unknown");denied(()->service.saveRecord(unavailable,null),"disabled post cannot record");check(service.evidence(recordId).length>0,"disabled post keeps historical evidence");
  service.savePost(postId,postInput("恢复岗位",3,"0","B"));unavailable.put("requestId",UUID.randomUUID().toString());var unknown=service.saveRecord(unavailable,null);check(unknown.get("result").equals("unknown"),"unavailable can be recorded as unknown without fabricated image");
  var invalid=new LinkedHashMap<>(unavailable);invalid.put("requestId",UUID.randomUUID().toString());denied(()->service.saveRecord(invalid,"not image".getBytes()),"invalid image rejected");
  db.execute("CREATE TRIGGER fixture_fail_inspection BEFORE INSERT ON fire_inspection_record FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture rollback'");
  long files=Files.list(evidence).count();try{service.saveRecord(invalid,picture);throw new AssertionError("trigger did not fail");}catch(org.springframework.dao.DataAccessException expected){checks++;}check(Files.list(evidence).count()==files,"failed DB transaction cleans evidence");check(db.queryForObject("SELECT COUNT(*) FROM fire_inspection_record",Integer.class)==2,"failed write leaves no record");
  System.out.println("INSPECTION_INTEGRATION_OK checks="+checks+"; actual MySQL, MyBatis, DataScopeAspect and transactions; no business DB, vendor API or live media accessed");
 }
}
