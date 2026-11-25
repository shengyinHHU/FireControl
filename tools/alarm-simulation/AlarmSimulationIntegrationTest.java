import java.util.*;
import java.util.concurrent.*;
import java.sql.*;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.aspectj.DataScopeAspect;
import com.ruoyi.system.service.*;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.fire.*;
import com.ruoyi.system.mapper.*;

/** Real transaction, DataScope, MyBatis union, idempotency and SSE checks in disposable MySQL. */
public class AlarmSimulationIntegrationTest {
 static int checks;
 static void ok(boolean condition,String name){if(!condition)throw new AssertionError(name);checks++;}
 static void denied(Runnable call,String name){boolean failed=false;try{call.run();}catch(com.ruoyi.common.exception.ServiceException expected){failed=true;}ok(failed,name);}
 static void login(long id,long dept){
  var user=new SysUser();user.setUserId(id);user.setUserName("fixture"+id);user.setDeptId(dept);
  var role=new SysRole();role.setRoleId(10L);role.setStatus("0");role.setDataScope("3");role.setPermissions(Set.of("fire:places:list","fire:alarms:list"));user.setRoles(List.of(role));
  var principal=new LoginUser(id,dept,user,Set.of("fire:places:list","fire:alarms:list","fire:alarms:simulate"));
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",List.of()));
 }
 static <T> T scoped(T target){var f=new AspectJProxyFactory(target);f.setProxyTargetClass(true);f.addAspect(new DataScopeAspect());return (T)f.getProxy();}
 static FireAlarmSimulationService.Create input(String request,long place,String source){return new FireAlarmSimulationService.Create(request,place,source,"模拟联动演示");}
 public static void main(String[] args)throws Exception{
  var attributes=new HashMap<String,Object>();
  org.springframework.web.context.request.RequestContextHolder.setRequestAttributes((org.springframework.web.context.request.RequestAttributes)java.lang.reflect.Proxy.newProxyInstance(AlarmSimulationIntegrationTest.class.getClassLoader(),new Class[]{org.springframework.web.context.request.RequestAttributes.class},(o,m,a)->{switch(m.getName()){case "getAttribute":return attributes.get(a[0]);case "setAttribute":attributes.put((String)a[0],a[1]);return null;case "removeAttribute":attributes.remove(a[0]);return null;case "getAttributeNames":return attributes.keySet().toArray(new String[0]);case "getSessionId":return "fixture";case "getSessionMutex":return attributes;default:return null;}}));
  var ds=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","root","");
  var db=new JdbcTemplate(ds);
  db.execute("CREATE TABLE IF NOT EXISTS fire_place_contact(contact_id bigint PRIMARY KEY,place_id bigint,contact_name varchar(100),phone varchar(64),contact_role varchar(32),status char(1),del_flag char(1),sort_order int)");
  db.update("INSERT INTO fire_place(place_id,name,place_type,dept_id,supervisor_user_id,address,longitude,latitude,coordinate_system,location_status) VALUES(9001,'演示场所','industry',100,22,'演示路1号',118.75,32.22,'GCJ02','review'),(9002,'外部门场所','industry',999,99,'其他路',118.75,32.22,'GCJ02','confirmed'),(9003,'无坐标场所','industry',100,22,'演示路3号',NULL,NULL,'GCJ02','pending'),(9004,'范围外场所','industry',100,22,'范围外',118.9,32.33,'GCJ02','confirmed')");
  db.update("INSERT INTO fire_gateway(gateway_id,place_id,name,source_address,center_address,allowed_ip,dept_id,supervisor_user_id) VALUES(9001,9001,'真实装置测试夹具','ee0000000000','640000000000','127.0.0.99',100,22)");
  db.update("INSERT INTO fire_event(event_id,gateway_id,message_id,object_type,object_index,object_key,event_kind,name,received_at,details_json) VALUES('fixture-real-device',9001,1,21,0,'gateway:fixture','fire','真实设备火警夹具',NOW(),'{}')");
  var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);factory.setMapperLocations(new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/Fire119Mapper.xml"),new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FirePlaceMapper.xml"));
  var session=new SqlSessionTemplate(factory.getObject());
  var placeTarget=new com.ruoyi.system.service.impl.FirePlaceServiceImpl();var field=placeTarget.getClass().getDeclaredField("placeMapper");field.setAccessible(true);field.set(placeTarget,session.getMapper(FirePlaceMapper.class));
  IFirePlaceService places=scoped(placeTarget);
  var cfg=new Fire119Properties();cfg.setSourceSystem("fixture_119");cfg.setBoundaryCoordinateMode("verified");cfg.setSourceCoordinateSystem("GCJ02");
  var alarms=scoped(new FireAlarmQueryService(session.getMapper(Fire119Mapper.class),cfg));
  var updates=new FireAlarmUpdates();
  var ownerHints=new CopyOnWriteArrayList<String>();var otherHints=new CopyOnWriteArrayList<String>();
  updates.subscribe(22L,Set.of(),new FireAlarmUpdates.Sink(){public void send(String e){ownerHints.add(e);}public void close(){}});
  updates.subscribe(99L,Set.of(),new FireAlarmUpdates.Sink(){public void send(String e){otherHints.add(e);}public void close(){}});
  var target=new FireAlarmSimulationService(ds,places,alarms,new Fire119Boundary(cfg,new tools.jackson.databind.ObjectMapper()),updates);
  var tx=new TransactionProxyFactoryBean();tx.setTarget(target);tx.setProxyTargetClass(true);tx.setTransactionManager(new DataSourceTransactionManager(ds));var attrs=new Properties();attrs.setProperty("*","PROPAGATION_REQUIRED,-java.lang.Exception");tx.setTransactionAttributes(attrs);tx.afterPropertiesSet();var service=(FireAlarmSimulationService)tx.getObject();
  long realEvents=db.queryForObject("SELECT COUNT(*) FROM fire_event",Long.class),real119=db.queryForObject("SELECT COUNT(*) FROM fire_119_incident",Long.class);
  try{
   login(22,100);
   var realQuery=new FireAccessQuery();realQuery.setId("fixture-real-device");var real=alarms.alarms(realQuery).get(0);
   ok(real.get("lng")!=null&&real.get("lat")!=null&&!Boolean.TRUE.equals(real.get("isSimulation")),"real device alarm inherits its associated place coordinates without a simulation marker");
   String request=UUID.randomUUID().toString();var first=service.create(input(request,9001,"device"));String id=first.get("id").toString();
   ok(id.startsWith("sim:"),"separate prefixed simulation identity");
   ok(service.create(input(request,9001,"device")).get("id").equals(id),"retry returns the original simulation");
   denied(()->service.create(input(request,9001,"119")),"same request cannot create different content");
   denied(()->service.create(input(UUID.randomUUID().toString(),9002,"device")),"out-of-scope place rejected");
   denied(()->service.create(input(UUID.randomUUID().toString(),9003,"device")),"missing coordinates rejected");
   denied(()->service.create(input(UUID.randomUUID().toString(),9004,"device")),"outside coordinates rejected");
   denied(()->service.create(input("invalid",9001,"device")),"invalid request identity rejected");
   var q=new FireAccessQuery();q.setId(id);var row=alarms.alarms(q).get(0);
   ok(Boolean.TRUE.equals(row.get("isSimulation"))&&row.get("lng")!=null&&row.get("name").toString().startsWith("【模拟演示】"),"scoped unified query retains mark and position");
   var rescue=service.create(input(UUID.randomUUID().toString(),9001,"119"));var source=new FireAccessQuery();source.setSource("119");
   ok(alarms.alarms(source).stream().anyMatch(x->x.get("id").equals(rescue.get("id"))&&"测试".equals(x.get("caseNatureName"))),"119 simulation supports source filtering and test nature");
   login(99,999);ok(alarms.alarms(q).isEmpty(),"detail cannot bypass department scope");denied(()->service.handle(id,new FireAlarmSimulationService.Handle("外部门处理")),"handling cannot bypass scope");
   login(22,100);denied(()->service.handle(id,new FireAlarmSimulationService.Handle(" ")),"handling note required");
   service.handle(id,new FireAlarmSimulationService.Handle("现场核查完成，演示结束"));row=alarms.alarms(q).get(0);
   ok(row.get("simulationHandledAt")!=null&&"现场核查完成，演示结束".equals(row.get("simulationNote")),"handling persisted in the normal detail response");
   service.handle(id,new FireAlarmSimulationService.Handle("重复提交不会改写"));ok("现场核查完成，演示结束".equals(alarms.alarms(q).get(0).get("simulationNote")),"duplicate handling preserves first result");
   ok(db.queryForObject("SELECT COUNT(*) FROM fire_event",Long.class)==realEvents&&db.queryForObject("SELECT COUNT(*) FROM fire_119_incident",Long.class)==real119,"real ingestion tables unchanged");
   var flush=FireAlarmUpdates.class.getDeclaredMethod("flush");flush.setAccessible(true);flush.invoke(updates);
   ok(ownerHints.contains("alarm-change")&&!otherHints.contains("alarm-change"),"committed simulation hints contain no payload and target owner streams only");
   // Transaction rollback must leave neither a row nor a new hint.
   ownerHints.clear();var template=new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(ds));
   long before=db.queryForObject("SELECT COUNT(*) FROM fire_alarm_simulation",Long.class);
   template.execute(status->{service.create(input(UUID.randomUUID().toString(),9001,"device"));status.setRollbackOnly();return null;});flush.invoke(updates);
   ok(db.queryForObject("SELECT COUNT(*) FROM fire_alarm_simulation",Long.class)==before&&!ownerHints.contains("alarm-change"),"rollback suppresses row and signal");
   var annotation=com.ruoyi.web.controller.fire.FireAlarmSimulationController.class.getMethod("create",FireAlarmSimulationService.Create.class).getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
   ok(annotation.contains("fire:alarms:simulate")&&annotation.contains("fire:places:list")&&annotation.contains("fire:alarms:list"),"create endpoint requires three explicit permissions");
   System.out.println("ALARM_SIMULATION_INTEGRATION_OK checks="+checks);
  }finally{updates.destroy();SecurityContextHolder.clearContext();org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();}
 }
}
