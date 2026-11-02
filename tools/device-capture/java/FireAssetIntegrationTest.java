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
 static FireAsset asset(String code,String source){var a=new FireAsset();a.name="测试用传"+code;a.unitName="独立测试单位";a.deviceCode=code;a.manufacturer="浙江大华";a.model="DH-HY-FAM-1000";a.enabled="1";a.sourceAddress=source;a.centerAddress="640000000000";a.allowedIp=null;return a;}
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
   a.enabled="0";a.centerAddress=null;a.allowedIp=null;service.saveGateway(a);a.revision=1L;
   ok("0".equals(service.gateway(a.id).get("enabled")),"enabled gateway accepts all three identity fields empty");
   ok("未收到报文".equals(service.gateway(a.id).get("status")),"communication status reflects no traffic rather than unknown identity");
   a.sourceAddress="c90000000000";a.allowedIp="127.0.0.3";a.centerAddress="640000000000";service.saveGateway(a);a.revision=2L;
   var b=asset("fixture-2","c90000000000");b.allowedIp="127.0.0.4";b.enabled="0";b.id=service.saveGateway(b);b.revision=0L;
   denied(()->service.saveGateway(asset("fixture-2","cb0000000000")),"business code unique");
   var wrongIp=asset("bad-ip",null);wrongIp.allowedIp="10.5.90.999";denied(()->service.saveGateway(wrongIp),"invalid IP rejected");
   long eventsBefore=scalar(db,"SELECT COUNT(*) FROM fire_event");
   var tcp=new FireTcpServer(props,ingest,connections);
   try{tcp.start();try(var one=new Socket("127.0.0.1",port,InetAddress.getByName(a.allowedIp),0);var two=new Socket("127.0.0.1",port,InetAddress.getByName(b.allowedIp),0)){
    one.setSoTimeout(3000);two.setSoTimeout(3000);one.getOutputStream().write(frame(a.sourceAddress));two.getOutputStream().write(frame(b.sourceAddress));
    ok(Gb26875.decode(one.getInputStream().readNBytes(30)).command()==3,"first gateway ACK");ok(Gb26875.decode(two.getInputStream().readNBytes(30)).command()==3,"second gateway ACK");
    ok(connections.online(a.allowedIp)&&connections.online(b.allowedIp),"two sources simultaneously online");
    ok("在线".equals(service.gateway(a.id).get("status"))&&service.gateway(a.id).get("lastSeen")!=null,"actual valid socket report updates communication status and receipt time");
    ok(scalar(db,"SELECT COUNT(*) FROM fire_event")==eventsBefore+2,"identical object from two sources retained separately");
    a.enabled="1";service.saveGateway(a);a.revision=3L;ok(!connections.online(a.allowedIp)&&connections.online(b.allowedIp),"disable invalidates only matching source");ok(one.getInputStream().read()==-1,"disabled socket closed after commit");
    ok(FireAccessIntegrationTest.ingest(ingest,frame(a.sourceAddress),a.allowedIp).reply()==null,"trusted discovery cannot re-enable explicitly disabled device");
    two.getOutputStream().write(frame(b.sourceAddress));ok(Gb26875.decode(two.getInputStream().readNBytes(30)).command()==3,"other gateway continues receiving and ACK");
    ok(scalar(db,"SELECT COUNT(*) FROM fire_event")==eventsBefore+2,"retransmit deduplicated per source");
   }}finally{tcp.stop();}
   var old=asset(a.deviceCode,a.sourceAddress);old.id=a.id;old.revision=1L;denied(()->service.saveGateway(old),"stale revision rejected");
   long history=scalar(db,"SELECT COUNT(*) FROM fire_event WHERE gateway_id="+a.id);service.deleteGateway(a.id,a.revision);
   denied(()->service.gateway(a.id),"deleted gateway hidden from active CRUD");var receipt=FireAccessIntegrationTest.ingest(ingest,frame(a.sourceAddress),a.allowedIp);ok(receipt.source()==null&&receipt.reply()==null,"deleted device cannot ACK or become online");
   var historyQuery=new FireAccessQuery();historyQuery.setGatewayId(String.valueOf(a.id));ok(mapper.events(historyQuery).size()==history,"history retained after gateway deletion");
   var h=new FireAsset();h.name="台账主机";h.gatewayId=b.id;h.manufacturer="北大青鸟";h.id=service.saveHost(h);h.revision=0L;h.model="JB-TG-JBF-11S";service.saveHost(h);h.revision=1L;ok(scalar(db,"SELECT COUNT(*) FROM fire_host WHERE host_id="+h.id+" AND system_address IS NULL")==1,"draft host metadata CRUD without fabricated protocol identity");
   h.systemType=1;h.systemAddress=null;denied(()->service.saveHost(h),"paired system identity validation");h.systemType=null;service.deleteHost(h.id,h.gatewayId,h.revision);
   byte[] host=FireAccessIntegrationTest.frame(401,1,new byte[]{1,1,3,0,1,0,12,20,10,26});System.arraycopy(Gb26875.unhex(b.sourceAddress),0,host,12,6);int sum=0;for(int i=2;i<host.length-3;i++)sum+=Gb26875.u(host[i]);host[host.length-3]=(byte)sum;
   FireAccessIntegrationTest.ingest(ingest,host,b.allowedIp);long hostId=scalar(db,"SELECT host_id FROM fire_host WHERE gateway_id="+b.id+" AND system_type=1");service.deleteHost(hostId,b.id,0L);FireAccessIntegrationTest.ingest(ingest,host,b.allowedIp);ok(scalar(db,"SELECT COUNT(*) FROM fire_host WHERE host_id="+hostId+" AND del_flag='2'")==1,"automatic discovery never resurrects deleted host");ok(scalar(db,"SELECT COUNT(*) FROM fire_event WHERE gateway_id="+b.id+" AND host_id="+hostId)>0,"deleted host reports still retained as events");
   login(2,200,"5");denied(()->service.gateway(b.id),"real DataScope blocks other owner reads");denied(()->service.saveHost(h),"cannot attach host to forbidden gateway");
   var own=asset("fixture-own",null);own.placeId=102L;denied(()->service.saveGateway(own),"cannot bind a place outside current place permission scope");own.placeId=null;own.id=service.saveGateway(own);var q=new FireAccessQuery();q.getParams().put("dataScope"," OR 1=1");ok(reads.gateways(q).size()==1,"caller dataScope discarded; only own unlinked device visible");denied(()->service.unregistered(),"unregistered traffic restricted to admin");
   login(3,100,"3");ok(reads.gateways(new FireAccessQuery()).stream().anyMatch(row->String.valueOf(b.id).equals(row.get("id"))),"department scope works on gateway ownership");
   login(1,100,"1");ok(connections.generation(b.allowedIp)==0,"unrelated source generation untouched");connections.invalidate(b.allowedIp);ok(!connections.seenCurrent(b.allowedIp,"stale",0),"stale post-commit receipt cannot restore online");
   var unknown=frame("cc0000000000");var discovered=FireAccessIntegrationTest.ingest(ingest,unknown,"10.5.90.77");
   ok(discovered.reply()!=null&&"cc0000000000".equals(discovered.source()),"unknown trusted source automatically registered and confirmed");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE source_address='cc0000000000' AND center_address='640000000000' AND allowed_ip='10.5.90.77' AND enabled='0' AND place_id IS NULL AND unit_name='待关联单位' AND supervisor_user_id=1")==1,"learn observed identity without guessing unit or place");
   long discoveredEvents=scalar(db,"SELECT COUNT(*) FROM fire_event");
   FireAccessIntegrationTest.ingest(ingest,unknown,"10.5.90.78");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE source_address='cc0000000000'")==2&&scalar(db,"SELECT COUNT(*) FROM fire_event")==discoveredEvents+1,"same protocol address from different IPs creates separate devices and events");
   byte[] otherCenter=unknown.clone();otherCenter[18]=0x65;int centerSum=0;for(int i=2;i<otherCenter.length-3;i++)centerSum+=Gb26875.u(otherCenter[i]);otherCenter[otherCenter.length-3]=(byte)centerSum;
   ok(FireAccessIntegrationTest.ingest(ingest,otherCenter,"10.5.90.79").reply()!=null,"center address no longer acts as access filter");
   var pool=java.util.concurrent.Executors.newFixedThreadPool(4);
   try{var tasks=new java.util.ArrayList<java.util.concurrent.Future<FireIngestService.Receipt>>();
    for(int i=0;i<4;i++)tasks.add(pool.submit(()->FireAccessIntegrationTest.ingest(ingest,frame("d00000000000"),"10.5.90.80")));
    for(var task:tasks)ok(task.get(15,java.util.concurrent.TimeUnit.SECONDS).reply()!=null,"concurrent first report confirmed");
   }finally{pool.shutdownNow();}
   ok(scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE source_address='d00000000000'")==1&&scalar(db,"SELECT COUNT(*) FROM fire_event e JOIN fire_gateway g ON g.gateway_id=e.gateway_id WHERE g.source_address='d00000000000'")==1,"concurrent discovery creates one device and deduplicates its events");
   var corrupt=frame("cd0000000000");corrupt[corrupt.length-3]^=1;
   ok(FireAccessIntegrationTest.ingest(ingest,corrupt,"10.5.90.81").reply()==null&&scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE source_address='cd0000000000'")==0,"corrupt first packet cannot register or receive success confirmation");
   var unsupported=frame("ce0000000000");unsupported[4]=2;int versionSum=0;for(int i=2;i<unsupported.length-3;i++)versionSum+=Gb26875.u(unsupported[i]);unsupported[unsupported.length-3]=(byte)versionSum;
   ok(FireAccessIntegrationTest.ingest(ingest,unsupported,"10.5.90.82").reply()==null&&scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE source_address='ce0000000000'")==0,"unsupported version does not register a source");
   long beforeNewFailure=scalar(db,"SELECT COUNT(*) FROM fire_protocol_message");
   try(var statement=db.createStatement()){statement.execute("CREATE TRIGGER fail_auto_event BEFORE INSERT ON fire_event FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced discovery rollback'");}
   boolean newFailed=false;try{FireAccessIntegrationTest.ingest(ingest,frame("cf0000000000"),"10.5.90.83");}catch(Exception e){newFailed=true;}
   try(var statement=db.createStatement()){statement.execute("DROP TRIGGER fail_auto_event");}
   ok(newFailed&&scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE source_address='cf0000000000'")==0&&scalar(db,"SELECT COUNT(*) FROM fire_protocol_message")==beforeNewFailure,"first report registration and messages roll back together before ACK");
   var inventory=asset("ip-206",null);inventory.name="凤滨嘉园用户信息传输装置";inventory.unitName="凤滨嘉园";inventory.allowedIp="10.6.89.246";inventory.centerAddress=null;inventory.enabled="0";inventory.id=service.saveGateway(inventory);
   long beforeBinding=scalar(db,"SELECT COUNT(*) FROM fire_gateway");
   var bound=FireAccessIntegrationTest.ingest(ingest,frame("d10000000000"),"10.6.89.246");
   ok(bound.reply()!=null&&scalar(db,"SELECT COUNT(*) FROM fire_gateway")==beforeBinding,"unique recorded IP binds to inventory without creating a duplicate device");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE gateway_id="+inventory.id+" AND source_address='d10000000000' AND center_address='640000000000' AND unit_name='凤滨嘉园' AND revision=1 AND last_received_at IS NOT NULL")==1,"identity and receipt learned while preserving inventory metadata");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_event WHERE gateway_id="+inventory.id)==1,"learned-IP alarm belongs to original inventory unit");
   var disabled=asset("ip-disabled",null);disabled.allowedIp="10.6.89.247";disabled.id=service.saveGateway(disabled);
   ok(FireAccessIntegrationTest.ingest(ingest,frame("d20000000000"),disabled.allowedIp).reply()==null&&scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE gateway_id="+disabled.id+" AND source_address='d20000000000' AND enabled='1'")==1,"learning a unique IP preserves manual disabled state");
   var duplicateIp=asset("ip-duplicate",null);duplicateIp.allowedIp=inventory.allowedIp;
   denied(()->service.saveGateway(duplicateIp),"a source IP can belong to only one inventory record");
   long beforeProtocolChange=scalar(db,"SELECT COUNT(*) FROM fire_gateway");
   ok(FireAccessIntegrationTest.ingest(ingest,frame("d30000000000"),inventory.allowedIp).reply()!=null&&scalar(db,"SELECT COUNT(*) FROM fire_gateway")==beforeProtocolChange,"same IP with changed protocol address remains the same device");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE gateway_id="+inventory.id+" AND source_address='d30000000000'")==1,"protocol address learned as metadata rather than device identity");
  }finally{SecurityContextHolder.clearContext();}
  System.out.println("FIRE_ASSET_INTEGRATION_OK checks="+checks);
 }
}
