import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.security.*;
import java.time.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import javax.crypto.Cipher;
import com.sun.net.httpserver.HttpServer;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import com.ruoyi.system.fire.*;
import com.ruoyi.system.mapper.Fire119Mapper;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.service.FireAlarmQueryService;
import tools.jackson.databind.ObjectMapper;

/** Disposable MySQL and loopback HTTP only. No real API credentials or device ports. */
public class Fire119IntegrationTest {
 static int checks=0;static Connection db;static ObjectMapper json;
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
 static long count(String table)throws Exception{try(var s=db.createStatement();var result=s.executeQuery("SELECT COUNT(*) FROM "+table)){result.next();return result.getLong(1);}}
 static Map<String,Object> fixture(String id,String type,boolean inside){
  Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("xzqybh","320116");m.put("ajlxdm",type);m.put("ajlxmc",type.equals("1")?"火警扑救":type.equals("2")?"抢险救援":"社会救助");m.put("ajztdm","30");m.put("ajztmc","出动");m.put("ajxz",1);m.put("ajxzmc","实警");m.put("hbzt",1);m.put("hbztmc","未合并");m.put("temp_ajdjdm",1);m.put("temp_ajdjdmmc","一级");m.put("afdz","隔离测试地址");m.put("ajms","隔离测试描述");m.put("jd",inside?"118.75163000":"118.90000000");m.put("wd",inside?"32.21230500":"32.33000000");m.put("lasj","2026-10-21 13:14:16");m.put("sjc","2026-10-21 13:36:09");m.put("create_time","2026-10-21T05:14:21.000+0000");m.put("njs_update_time","2026-10-21T05:25:31.000+0000");m.put("manually_add",false);m.put("counterfeit",false);m.put("flag",0);m.put("manual_operate_count",0);return m;
 }
 static FireAccessQuery query(){FireAccessQuery q=new FireAccessQuery();q.getParams().put("dataScope","");q.getParams().put("sourceSystem","fixture_119");return q;}
 static void reply(com.sun.net.httpserver.HttpExchange e,Object body)throws IOException{byte[] data=json.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,data.length);try(var out=e.getResponseBody()){out.write(data);}}
 public static void main(String[] args)throws Exception {
  var context=new org.springframework.context.annotation.AnnotationConfigApplicationContext();context.register(org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class);context.refresh();json=context.getBean(ObjectMapper.class);
  String url="jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
  db=DriverManager.getConnection(url,"root","");var ds=new DriverManagerDataSource(url,"root","");
  var f=new SqlSessionFactoryBean();f.setDataSource(ds);f.setMapperLocations(new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/Fire119Mapper.xml"));var session=new SqlSessionTemplate(f.getObject());var mapper=session.getMapper(Fire119Mapper.class);
  var cfg=new Fire119Properties();cfg.setSourceSystem("fixture_119");cfg.setRegionCode("320116");cfg.setOrderSource("FIXTURE");cfg.setAccount("FIXTURE");cfg.setPassword("fixture-only");cfg.setSourceCoordinateSystem("GCJ02");cfg.setBoundaryCoordinateMode("verified");cfg.setDeptId(100L);
  var boundary=new Fire119Boundary(cfg,json);check(boundary.match(new java.math.BigDecimal("118.75163"),new java.math.BigDecimal("32.212305")).status().equals("inside"),"document region inside");
  check(boundary.match(new java.math.BigDecimal("118.9"),new java.math.BigDecimal("32.33")).status().equals("outside"),"outside document polygons");
  var tri=List.of(new double[]{0,0},new double[]{2,0},new double[]{0,2},new double[]{0,0});check(Fire119Boundary.covers(tri,1,0),"boundary included");check(!Fire119Boundary.covers(tri,1.8,1.8),"no bbox false positive");
  check(Fire119IngestService.utc("2026-10-21T05:25:31.000+0000").equals(LocalDateTime.of(2026,10,21,5,25,31)),"explicit UTC source timestamp");
  var proxy=new TransactionProxyFactoryBean();proxy.setTarget(new Fire119IngestService(mapper,json,cfg,boundary));proxy.setTransactionManager(new DataSourceTransactionManager(ds));proxy.setProxyTargetClass(true);Properties tx=new Properties();tx.setProperty("*","PROPAGATION_REQUIRED,-java.lang.Exception");proxy.setTransactionAttributes(tx);proxy.afterPropertiesSet();var ingest=(Fire119IngestService)proxy.getObject();
  check(ingest.receive(fixture("one","1",true),"batch-one").equals("insert"),"first insert");check(count("fire_119_incident")==1 && count("fire_119_incident_history")==1,"initial snapshot and history");
  Map<String,Object> reordered=new TreeMap<>(fixture("one","1",true));check(ingest.receive(reordered,"batch-two").equals("unchanged"),"JSON order does not create history");check(count("fire_119_incident_history")==1,"repeat not duplicated");
  Map<String,Object> updated=fixture("one","1",true);updated.put("ajztdm","120");updated.put("ajztmc","返队");check(ingest.receive(updated,"batch-three").equals("update"),"source status update");check(count("fire_119_incident")==1 && count("fire_119_incident_history")==2,"same ID one incident, new history");
  ingest.receive(fixture("rescue","2",true),"batch-four");ingest.receive(fixture("help","3",true),"batch-four");ingest.receive(fixture("outside","1",false),"batch-four");
  var invalid=fixture("invalid","1",true);invalid.put("jd","bad");ingest.receive(invalid,"batch-four");
  var rows=mapper.alarms(query());check(rows.size()==3,"all three types inside only");check(rows.stream().allMatch(x->"119".equals(x.get("sourceCode"))),"source DTO");check(rows.stream().anyMatch(x->"返队".equals(x.get("status"))),"preserve case status");
  var scoped=query();scoped.getParams().put("dataScope"," AND a.dept_id=999");check(mapper.alarms(scoped).isEmpty(),"scope excludes unauthorized incidents");
  scoped=query();scoped.setSource("device");check(mapper.alarms(scoped).isEmpty(),"device source isolated");
  scoped=query();scoped.setCaseType("2");check(mapper.alarms(scoped).size()==1,"case type filter");
  scoped=query();scoped.setId(String.valueOf(rows.get(0).get("id")));check(mapper.alarms(scoped).size()==1,"prefixed detail ID");
  var service=new FireAlarmQueryService(mapper,cfg);check(service.alarms(query()).stream().allMatch(x->"database".equals(x.get("dataSource"))),"query normalized");
  try(var stmt=db.createStatement()){stmt.execute("CREATE TRIGGER fixture_fail_history BEFORE INSERT ON fire_119_incident_history FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture rollback'");}
  long before=count("fire_119_incident");boolean failed=false;try{ingest.receive(fixture("rollback","1",true),"failed");}catch(Exception e){failed=true;}check(failed && count("fire_119_incident")==before,"history failure rolls back snapshot");try(var stmt=db.createStatement()){stmt.execute("DROP TRIGGER fixture_fail_history");}
  KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);KeyPair pair=gen.generateKeyPair();cfg.setPublicKey(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
  String longPlain="分段兼容验证".repeat(40);byte[] encrypted=Base64.getDecoder().decode(Fire119Client.encryptSection(longPlain,cfg.getPublicKey()));Cipher decrypt=Cipher.getInstance("RSA");decrypt.init(Cipher.DECRYPT_MODE,pair.getPrivate());var plain=new ByteArrayOutputStream();for(int offset=0;offset<encrypted.length;offset+=256)plain.write(decrypt.doFinal(encrypted,offset,256));check(longPlain.equals(plain.toString(StandardCharsets.UTF_8)),"vendor 117 byte RSA chunk compatibility");
  AtomicInteger tokens=new AtomicInteger(),calls=new AtomicInteger();AtomicBoolean authFail=new AtomicBoolean(),pageFail=new AtomicBoolean(),slowBody=new AtomicBoolean();List<Throwable> errors=Collections.synchronizedList(new ArrayList<>());
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/generateToken",e->{try{
   check(e.getRequestMethod().equals("POST"),"token POST");var body=json.readValue(e.getRequestBody().readAllBytes(),Map.class);check("FIXTURE".equals(body.get("orderSource")),"token source");byte[] cipher=Base64.getDecoder().decode((String)body.get("secretKey"));var dec=Cipher.getInstance("RSA");dec.init(Cipher.DECRYPT_MODE,pair.getPrivate());var secret=json.readValue(dec.doFinal(cipher),Map.class);check("FIXTURE".equals(secret.get("account")) && "fixture-only".equals(secret.get("password")),"RSA credentials");check(Math.abs(Instant.now().getEpochSecond()-((Number)secret.get("timeStamp")).longValue())<5,"fresh Unix seconds");tokens.incrementAndGet();reply(e,Map.of("code",200,"data","fixture-token"));
  }catch(Throwable t){errors.add(t);reply(e,Map.of("code",500));}});
  server.createContext("/queryParamsXfjqxx",e->{try{
   if(slowBody.get()){
    e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,0);
    try(var output=e.getResponseBody()){output.write('{');output.flush();Thread.sleep(1800);output.write('}');}catch(IOException ignored){}return;
   }
   check("fixture-token".equals(e.getRequestHeaders().getFirst("token")),"screenshot token header");check("FIXTURE".equals(e.getRequestHeaders().getFirst("orderSource")),"screenshot orderSource header");check(e.getRequestHeaders().getFirst("Authorization")==null,"no guessed Bearer header");calls.incrementAndGet();
   if(authFail.getAndSet(false)){reply(e,Map.of("code",401));return;}
   Map<String,String> q=new HashMap<>();for(String item:e.getRequestURI().getQuery().split("&")){String[] kv=item.split("=");q.put(kv[0],kv[1]);}
   int page=Integer.parseInt(q.get("pageNum"));check("320116".equals(q.get("xzqybh")),"configured region");
   if(pageFail.get()&&page==2){reply(e,Map.of("code",500));return;}
   List<Map<String,Object>> data=page==1?List.of(fixture("network-1","1",true),fixture("network-2","3",false)):List.of(fixture("network-3","2",true));
   reply(e,Map.of("code",200,"data",Map.of("pageNum",page,"pageSize",2,"pages",2,"total",3,"list",data)));
  }catch(Throwable t){errors.add(t);reply(e,Map.of("code",500));}});
  server.start();
  try{
   cfg.setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());cfg.setPageSize(2);var client=new Fire119Client(cfg,json);
   client.page(1);client.page(2);check(tokens.get()==1,"token cached across pages");authFail.set(true);client.page(1);check(tokens.get()==2,"refresh token once on authentication failure");
   var sync=new Fire119Synchronizer(cfg,client,ingest);sync.syncOnce();check("success".equals(mapper.syncState("fixture_119","320116").get("lastResult")),"complete batch success");long histories=count("fire_119_incident_history");sync.syncOnce();check(count("fire_119_incident_history")==histories,"repeat HTTP batch no duplicated history");
   Object complete=mapper.syncState("fixture_119","320116").get("lastCompletedAt");pageFail.set(true);sync.syncOnce();var state=mapper.syncState("fixture_119","320116");check("failed".equals(state.get("lastResult")),"page failure reported");check(Objects.equals(complete,state.get("lastCompletedAt")),"failed batch does not advance complete time");
   check(errors.isEmpty(),"fixture handlers successful: "+errors);
   slowBody.set(true);cfg.setRequestTimeoutSeconds(1);long began=System.nanoTime();boolean timedOut=false;
   try{client.page(1);}catch(Exception expected){timedOut=true;}
   check(timedOut && (System.nanoTime()-began)<java.util.concurrent.TimeUnit.SECONDS.toNanos(3),"timeout covers stalled response body and is bounded");
  }finally{server.stop(0);db.close();context.close();}
  System.out.println("FIRE_119_INTEGRATION_OK checks="+checks+" tokenRequests="+tokens.get()+" pageRequests="+calls.get());
 }
}
