import com.ruoyi.system.fire.*;
import com.ruoyi.system.service.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.domain.*;
import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.aspectj.DataScopeAspect;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.jdbc.datasource.*;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import java.util.*;
import java.net.*;
import java.sql.*;

public class FireAssetIntegrationTest {
 static int checks;
 static void ok(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 static void denied(Runnable r,String label){boolean failed=false;try{r.run();}catch(com.ruoyi.common.exception.ServiceException e){failed=true;}ok(failed,label);}
 static void login(long id,long dept,String scope){
  var user=new SysUser();user.setUserId(id);user.setUserName("fixture"+id);user.setDeptId(dept);
  var role=new SysRole();role.setRoleId(10L);role.setStatus("0");role.setDataScope(scope);role.setPermissions(Set.of("fire:hosts:list","fire:hosts:add","fire:hosts:edit","fire:hosts:remove","fire:alarms:list"));user.setRoles(List.of(role));
  var principal=new LoginUser(id,dept,user,Set.of("fire:hosts:list","fire:hosts:add","fire:hosts:edit","fire:hosts:remove"));
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",List.of()));
 }
 static FireAsset asset(String code,String source){var a=new FireAsset();a.name="测试用传"+code;a.unitName="独立测试单位";a.deviceCode=code;a.manufacturer="浙江大华";a.model="DH-HY-FAM-1000";a.enabled="1";a.sourceAddress=source;a.centerAddress="640000000000";a.allowedIp="127.0.0.1";return a;}
 static byte[] frame(String source){byte[] b=FireAccessIntegrationTest.frame(301,21,new byte[]{3,1,0,12,20,10,26});System.arraycopy(Gb26875.unhex(source),0,b,12,6);int sum=0;for(int i=2;i<b.length-3;i++)sum+=Gb26875.u(b[i]);b[b.length-3]=(byte)sum;return b;}
 static long scalar(Connection c,String query)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(query)){r.next();return r.getLong(1);}}
 public static void main(String[] args)throws Exception{
  var attributes=new java.util.HashMap<String,Object>();
  org.springframework.web.context.request.RequestContextHolder.setRequestAttributes((org.springframework.web.context.request.RequestAttributes)java.lang.reflect.Proxy.newProxyInstance(FireAssetIntegrationTest.class.getClassLoader(),new Class[]{org.springframework.web.context.request.RequestAttributes.class},(o,m,a)->{switch(m.getName()){case "getAttribute":return attributes.get(a[0]);case "setAttribute":attributes.put((String)a[0],a[1]);return null;case "removeAttribute":attributes.remove(a[0]);return null;case "getAttributeNames":return attributes.keySet().toArray(new String[0]);case "getSessionId":return "fixture";case "getSessionMutex":return attributes;default:return null;}}));
  String url="jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
  var ds=new DriverManagerDataSource(url,"root","");var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);factory.setMapperLocations(new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FireAccessMapper.xml"),new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FireAssetMapper.xml"),new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FirePlaceMapper.xml"));var template=new SqlSessionTemplate(factory.getObject());var mapper=template.getMapper(FireAccessMapper.class);var assets=template.getMapper(FireAssetMapper.class);
  var props=new FireDeviceProperties();props.setEnabled(true);props.setBindAddress("127.0.0.1");props.setIdleSeconds(10);int port;try(var p=new ServerSocket(0)){port=p.getLocalPort();}props.setPort(port);
  var connections=new FireConnections(props);var scope=new AspectJProxyFactory(new FireAccessService(mapper,connections));scope.setProxyTargetClass(true);scope.addAspect(new DataScopeAspect());var reads=(FireAccessService)scope.getProxy();
  var placeTarget=new com.ruoyi.system.service.impl.FirePlaceServiceImpl();var placeField=placeTarget.getClass().getDeclaredField("placeMapper");placeField.setAccessible(true);placeField.set(placeTarget,template.getMapper(FirePlaceMapper.class));var placeProxy=new AspectJProxyFactory(placeTarget);placeProxy.setProxyTargetClass(true);placeProxy.addAspect(new DataScopeAspect());var places=(IFirePlaceService)placeProxy.getProxy();
  var assetTarget=new FireAssetService(assets,reads,connections);var assetPlaceField=FireAssetService.class.getDeclaredField("places");assetPlaceField.setAccessible(true);assetPlaceField.set(assetTarget,places);
  var tx=new Properties();tx.setProperty("*","PROPAGATION_REQUIRED");var proxy=new TransactionProxyFactoryBean();proxy.setTarget(assetTarget);proxy.setTransactionManager(new DataSourceTransactionManager(ds));proxy.setTransactionAttributes(tx);proxy.setProxyTargetClass(true);proxy.afterPropertiesSet();var service=(FireAssetService)proxy.getObject();
  var ingestProxy=new TransactionProxyFactoryBean();ingestProxy.setTarget(new FireIngestService(mapper,new tools.jackson.databind.ObjectMapper()));ingestProxy.setTransactionManager(new DataSourceTransactionManager(ds));ingestProxy.setTransactionAttributes(tx);ingestProxy.setProxyTargetClass(true);ingestProxy.afterPropertiesSet();var ingest=(FireIngestService)ingestProxy.getObject();
  try(var db=ds.getConnection()){
   login(1,100,"1");
   var a=asset("fixture-1",null);a.id=service.saveGateway(a);a.revision=0L;ok(service.gateway(a.id).get("placeName").equals(a.unitName),"unlinked gateway readable by its unit name");
   a.placeId=102L;
   a.enabled="0";denied(()->service.saveGateway(a),"cannot enable incomplete identity");a.sourceAddress="c90000000000";service.saveGateway(a);a.revision=1L;
   var b=asset("fixture-2","ca0000000000");b.enabled="0";b.id=service.saveGateway(b);b.revision=0L;
   denied(()->service.saveGateway(asset("fixture-2","cb0000000000")),"business code unique");
   var wrongIp=asset("bad-ip",null);wrongIp.allowedIp="10.5.90.999";denied(()->service.saveGateway(wrongIp),"invalid IP rejected");
   long eventsBefore=scalar(db,"SELECT COUNT(*) FROM fire_event");
   var tcp=new FireTcpServer(props,ingest,connections);
   try{tcp.start();try(var one=new Socket("127.0.0.1",port);var two=new Socket("127.0.0.1",port)){
    one.setSoTimeout(3000);two.setSoTimeout(3000);one.getOutputStream().write(frame(a.sourceAddress));two.getOutputStream().write(frame(b.sourceAddress));
    ok(Gb26875.decode(one.getInputStream().readNBytes(30)).command()==3,"first gateway ACK");ok(Gb26875.decode(two.getInputStream().readNBytes(30)).command()==3,"second gateway ACK");
    ok(connections.online(a.sourceAddress)&&connections.online(b.sourceAddress),"two sources simultaneously online");
    ok(scalar(db,"SELECT COUNT(*) FROM fire_event")==eventsBefore+2,"identical object from two sources retained separately");
    a.enabled="1";service.saveGateway(a);a.revision=2L;ok(!connections.online(a.sourceAddress)&&connections.online(b.sourceAddress),"disable invalidates only matching source");ok(one.getInputStream().read()==-1,"disabled socket closed after commit");
    two.getOutputStream().write(frame(b.sourceAddress));ok(Gb26875.decode(two.getInputStream().readNBytes(30)).command()==3,"other gateway continues receiving and ACK");
    ok(scalar(db,"SELECT COUNT(*) FROM fire_event")==eventsBefore+2,"retransmit deduplicated per source");
   }}finally{tcp.stop();}
   var old=asset(a.deviceCode,a.sourceAddress);old.id=a.id;old.revision=1L;denied(()->service.saveGateway(old),"stale revision rejected");
   long history=scalar(db,"SELECT COUNT(*) FROM fire_event WHERE gateway_id="+a.id);service.deleteGateway(a.id,a.revision);
   denied(()->service.gateway(a.id),"deleted gateway hidden from active CRUD");var receipt=FireAccessIntegrationTest.ingest(ingest,frame(a.sourceAddress),"127.0.0.1");ok(receipt.source()==null&&receipt.reply()==null,"deleted device cannot ACK or become online");
   var historyQuery=new FireAccessQuery();historyQuery.setGatewayId(String.valueOf(a.id));ok(mapper.events(historyQuery).size()==history,"history retained after gateway deletion");
   var h=new FireAsset();h.name="台账主机";h.gatewayId=b.id;h.manufacturer="北大青鸟";h.id=service.saveHost(h);h.revision=0L;h.model="JB-TG-JBF-11S";service.saveHost(h);h.revision=1L;ok(scalar(db,"SELECT COUNT(*) FROM fire_host WHERE host_id="+h.id+" AND system_address IS NULL")==1,"draft host metadata CRUD without fabricated protocol identity");
   h.systemType=1;h.systemAddress=null;denied(()->service.saveHost(h),"paired system identity validation");h.systemType=null;service.deleteHost(h.id,h.gatewayId,h.revision);
   byte[] host=FireAccessIntegrationTest.frame(401,1,new byte[]{1,1,3,0,1,0,12,20,10,26});System.arraycopy(Gb26875.unhex(b.sourceAddress),0,host,12,6);int sum=0;for(int i=2;i<host.length-3;i++)sum+=Gb26875.u(host[i]);host[host.length-3]=(byte)sum;
   FireAccessIntegrationTest.ingest(ingest,host,"127.0.0.1");long hostId=scalar(db,"SELECT host_id FROM fire_host WHERE gateway_id="+b.id+" AND system_type=1");service.deleteHost(hostId,b.id,0L);FireAccessIntegrationTest.ingest(ingest,host,"127.0.0.1");ok(scalar(db,"SELECT COUNT(*) FROM fire_host WHERE host_id="+hostId+" AND del_flag='2'")==1,"automatic discovery never resurrects deleted host");ok(scalar(db,"SELECT COUNT(*) FROM fire_event WHERE gateway_id="+b.id+" AND host_id="+hostId)>0,"deleted host reports still retained as events");
   login(2,200,"5");denied(()->service.gateway(b.id),"real DataScope blocks other owner reads");denied(()->service.saveHost(h),"cannot attach host to forbidden gateway");
   var own=asset("fixture-own",null);own.placeId=102L;denied(()->service.saveGateway(own),"cannot bind a place outside current place permission scope");own.placeId=null;own.id=service.saveGateway(own);var q=new FireAccessQuery();q.getParams().put("dataScope"," OR 1=1");ok(reads.gateways(q).size()==1,"caller dataScope discarded; only own unlinked device visible");denied(()->service.unregistered(),"unregistered traffic restricted to admin");
   login(3,100,"3");ok(reads.gateways(new FireAccessQuery()).stream().anyMatch(row->String.valueOf(b.id).equals(row.get("id"))),"department scope works on gateway ownership");
   login(1,100,"1");ok(connections.generation(b.sourceAddress)==0,"unrelated source generation untouched");connections.invalidate(b.sourceAddress);ok(!connections.seenCurrent(b.sourceAddress,"stale",0),"stale post-commit receipt cannot restore online");
   var unknown=frame("cc0000000000");FireAccessIntegrationTest.ingest(ingest,unknown,"127.0.0.1");ok(service.unregistered().stream().anyMatch(row->"cc0000000000".equals(row.get("sourceAddress"))),"unknown source visible for explicit registration");
  }finally{SecurityContextHolder.clearContext();}
  System.out.println("FIRE_ASSET_INTEGRATION_OK checks="+checks);
 }
}
