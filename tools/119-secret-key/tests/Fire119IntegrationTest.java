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
 static FireAccessQuery query(){FireAccessQuery q=new FireAccessQuery();q.getParams().put("dataScope","");q.getParams().put("sourceSystem","fixture_119");q.getParams().put("geofenceVersion",Fire119Boundary.scopeVersion("WGS84"));q.getParams().put("dachangOnly",true);return q;}
 static void reply(com.sun.net.httpserver.HttpExchange e,Object body)throws IOException{byte[] data=json.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,data.length);try(var out=e.getResponseBody()){out.write(data);}}
 public static void main(String[] args)throws Exception {
  var context=new org.springframework.context.annotation.AnnotationConfigApplicationContext();context.register(org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class);context.refresh();json=context.getBean(ObjectMapper.class);
  String url="jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
  AtomicBoolean failAcquire=new AtomicBoolean();
  db=DriverManager.getConnection(url,"root","");var ds=new DriverManagerDataSource(url,"root",""){
   @Override public Connection getConnection() throws SQLException {if(failAcquire.get())throw new SQLException("fixture connection unavailable","08001");return super.getConnection();}
  };
  var f=new SqlSessionFactoryBean();f.setDataSource(ds);f.setMapperLocations(new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/Fire119Mapper.xml"));var session=new SqlSessionTemplate(f.getObject());var realMapper=session.getMapper(Fire119Mapper.class);
  AtomicInteger hashReads=new AtomicInteger(),lockReads=new AtomicInteger(),pageTouches=new AtomicInteger();
  var mapper=(Fire119Mapper)java.lang.reflect.Proxy.newProxyInstance(Fire119Mapper.class.getClassLoader(),new Class[]{Fire119Mapper.class},(obj,method,values)->{
   if(method.getName().equals("incidentHashes"))hashReads.incrementAndGet();if(method.getName().equals("incidentForUpdate"))lockReads.incrementAndGet();if(method.getName().equals("touchIncidents"))pageTouches.incrementAndGet();
   try{return method.invoke(realMapper,values);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
  });
  var cfg=new Fire119Properties();cfg.setSourceSystem("fixture_119");cfg.setRegionCode("320116");cfg.setOrderSource("FIXTURE");cfg.setAccount("FIXTURE");cfg.setPassword("fixture-only");cfg.setSourceCoordinateSystem("WGS84");cfg.setBoundaryCoordinateMode("verified");cfg.setDeptId(100L);
  var boundary=new Fire119Boundary(cfg,json);check(boundary.match(new java.math.BigDecimal("118.75163"),new java.math.BigDecimal("32.212305")).status().equals("inside"),"new 33-point region inside");
  check(boundary.match(new java.math.BigDecimal("118.9"),new java.math.BigDecimal("32.33")).status().equals("outside"),"outside new 33-point polygon");
  var tri=List.of(new double[]{0,0},new double[]{2,0},new double[]{0,2},new double[]{0,0});check(Fire119Boundary.covers(tri,1,0),"boundary included");check(!Fire119Boundary.covers(tri,1.8,1.8),"no bbox false positive");
  check(Fire119IngestService.utc("2026-10-21T05:25:31.000+0000").equals(LocalDateTime.of(2026,10,21,5,25,31)),"explicit UTC source timestamp");
  check(boundary.match(new java.math.BigDecimal("118.777719"),new java.math.BigDecimal("32.265056")).status().equals("inside"),"new polygon vertex included");
  var comparison=json.readValue(Files.readString(Path.of(args[1],"docs/verification/119-coordinate-comparison.json")),Map.class);
  double maxFormulaError=0;
  for(Object value:(List<?>)comparison.get("results")){var sample=(Map<?,?>)value;var source=(List<?>)sample.get("source");var expected=(List<?>)sample.get("local");var actual=Fire119Coordinates.toMap(new java.math.BigDecimal(source.get(0).toString()),new java.math.BigDecimal(source.get(1).toString()),"WGS84");for(int axis=0;axis<2;axis++)maxFormulaError=Math.max(maxFormulaError,Math.abs(actual[axis].doubleValue()-((Number)expected.get(axis)).doubleValue()));}
  check(maxFormulaError<0.00000001,"Java formula matches all 225 local results from live official comparison");
  var identity=Fire119Coordinates.toMap(new java.math.BigDecimal("118.75"),new java.math.BigDecimal("32.22"),"GCJ02");check(identity[0].toString().equals("118.75"),"GCJ02 input is never converted twice");
  var proxy=new TransactionProxyFactoryBean();proxy.setTarget(new Fire119IngestService(mapper,json,cfg,boundary));proxy.setTransactionManager(new DataSourceTransactionManager(ds));proxy.setProxyTargetClass(true);Properties tx=new Properties();tx.setProperty("*","PROPAGATION_REQUIRED,-java.lang.Exception");proxy.setTransactionAttributes(tx);proxy.afterPropertiesSet();var ingest=(Fire119IngestService)proxy.getObject();
  var converted=ingest.prepare(fixture("coordinate-only","1",true),"check");check(!converted.get("source_longitude").equals(converted.get("map_longitude"))&&"WGS84".equals(converted.get("source_coord_system")),"original WGS84 is separate from GCJ02 map point");
  var missing=fixture("missing-check","1",true);missing.remove("wd");check(ingest.prepare(missing,"check").get("geofence_status").equals("pending"),"missing coordinate stays pending");
  var invalidNumber=fixture("invalid-check","1",true);invalidNumber.put("jd","NaN");check(ingest.prepare(invalidNumber,"check").get("coordinate_status").equals("invalid"),"nonfinite coordinate rejected");
  boolean rawDifferenceTested=false;
  for(Object value:(List<?>)comparison.get("results")){var sample=(Map<?,?>)value;var source=(List<?>)sample.get("source");var rawPoint=fixture("crossing","1",true);rawPoint.put("jd",source.get(0));rawPoint.put("wd",source.get(1));var prepared=ingest.prepare(rawPoint,"check");String rawStatus=boundary.match(new java.math.BigDecimal(source.get(0).toString()),new java.math.BigDecimal(source.get(1).toString())).status();if(!rawStatus.equals(prepared.get("geofence_status"))){rawDifferenceTested=true;check(boundary.match((java.math.BigDecimal)prepared.get("map_longitude"),(java.math.BigDecimal)prepared.get("map_latitude")).status().equals(prepared.get("geofence_status")),"polygon check uses converted point, not raw WGS84");break;}}
  check(rawDifferenceTested,"conversion is material to range membership");
  check(ingest.receive(fixture("one","1",true),"batch-one").equals("insert"),"first insert");check(count("fire_119_incident")==1 && count("fire_119_incident_history")==1,"initial snapshot and history");
  Map<String,Object> reordered=new TreeMap<>(fixture("one","1",true));check(ingest.receive(reordered,"batch-two").equals("unchanged"),"JSON order does not create history");check(count("fire_119_incident_history")==1,"repeat not duplicated");
  Map<String,Object> updated=fixture("one","1",true);updated.put("ajztdm","120");updated.put("ajztmc","返队");check(ingest.receive(updated,"batch-three").equals("update"),"source status update");check(count("fire_119_incident")==1 && count("fire_119_incident_history")==2,"same ID one incident, new history");
  ingest.receive(fixture("rescue","2",true),"batch-four");ingest.receive(fixture("help","3",true),"batch-four");ingest.receive(fixture("outside","1",false),"batch-four");
  var invalid=fixture("invalid","1",true);invalid.put("jd","bad");ingest.receive(invalid,"batch-four");
  var allQuery=query();allQuery.getParams().put("dachangOnly",false);
  var unfiltered=mapper.alarms(allQuery);check(unfiltered.size()==5,"all scope includes outside and invalid-coordinate incidents");
  var deniedAll=query();deniedAll.getParams().put("dachangOnly",false);deniedAll.getParams().put("dataScope"," AND a.dept_id=999");check(mapper.alarms(deniedAll).isEmpty(),"unfiltered scope still enforces permission");
  var rows=mapper.alarms(query());check(rows.size()==3,"all three types inside only");check(rows.stream().allMatch(x->String.valueOf(x.get("streetRegion")).contains("33点范围")&&String.valueOf(x.get("streetRegion")).endsWith("地区=大厂街道")),"DTO region describes current geometric scope rather than old attribution");check(rows.stream().allMatch(x->"119".equals(x.get("sourceCode"))),"source DTO");check(rows.stream().anyMatch(x->"返队".equals(x.get("status"))),"preserve case status");
  var scoped=query();scoped.getParams().put("dataScope"," AND a.dept_id=999");check(mapper.alarms(scoped).isEmpty(),"scope excludes unauthorized incidents");
  scoped=query();scoped.setSource("device");check(mapper.alarms(scoped).isEmpty(),"device source isolated");
  scoped=query();scoped.setCaseType("2");check(mapper.alarms(scoped).size()==1,"case type filter");
  scoped=query();scoped.setId(String.valueOf(rows.get(0).get("id")));check(mapper.alarms(scoped).size()==1,"prefixed detail ID");
  cfg.setDachangOnly(false);
  var service=new FireAlarmQueryService(mapper,cfg);
  var malicious=query();malicious.getParams().put("dachangOnly",true);check(service.alarms(malicious).size()==5,"server config owns geographic filter");
  var summaryStatus=service.status(query());check(Boolean.FALSE.equals(summaryStatus.get("dachangOnly")) && summaryStatus.get("latestReceivedAt")!=null && summaryStatus.get("latestAlarmTime")!=null,"status reports full scope and latest timestamps");check(service.alarms(query()).stream().allMatch(x->"database".equals(x.get("dataSource"))),"query normalized");
  // Rebuild historical rows even when they no longer occur in upstream pages, without replaying alarms.
  cfg.setDachangOnly(true);long historyBeforeRebuild=count("fire_119_incident_history");
  try(var stmt=db.createStatement()){stmt.execute("UPDATE fire_119_incident SET geofence_version='old',source_coord_system='GCJ02',map_longitude=source_longitude,map_latitude=source_latitude,last_seen_at='2020-01-01 00:00:00',last_changed_at='2020-01-01 00:00:00'");stmt.execute("UPDATE fire_119_incident SET street_match_method='manual',street_name='人工归属保留',street_match_status='inside' WHERE external_id='outside'");}
  check(service.alarms(query()).isEmpty(),"unrebuilt old-version rows cannot leak through strict filter");
  check(((Number)service.status(query()).get("pendingCount")).intValue()==5,"old versions counted as pending rather than inside");
  check(ingest.rebuildCoordinates(2,Long.MAX_VALUE)==2,"bounded historical batch");
  while(ingest.rebuildCoordinates(2,Long.MAX_VALUE)>0){}
  check(service.alarms(query()).size()==3,"rebuild restores only converted in-scope incidents");
  var bypass=query();bypass.getParams().put("dachangOnly",false);bypass.getParams().put("geofenceVersion","old");check(service.alarms(bypass).size()==3,"client cannot disable geometric filtering or select an old rule version");
  var outsideDetail=query();outsideDetail.setId(String.valueOf(unfiltered.stream().filter(x->"outside".equals(x.get("code"))).findFirst().orElseThrow().get("id")));check(service.alarms(outsideDetail).isEmpty(),"outside incident cannot bypass filtering through detail id");

  check(ingest.rebuildCoordinates(2,Long.MAX_VALUE)==0,"repeat rebuild has no work");
  var staleGeometry=ingest.prepare(fixture("one","1",true),"scope-race");try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT incident_id FROM fire_119_incident WHERE external_id='one'")){rs.next();staleGeometry.put("incident_id",rs.getLong(1));}staleGeometry.put("expected_hash","0".repeat(64));check(mapper.rebuildCoordinates(staleGeometry)==0,"stale historical payload cannot overwrite concurrent source update");

  check(count("fire_119_incident_history")==historyBeforeRebuild,"rebuild creates no new event or revision history");
  try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT COUNT(*) FROM fire_119_incident WHERE last_seen_at='2020-01-01 00:00:00' AND last_changed_at='2020-01-01 00:00:00'")){rs.next();check(rs.getInt(1)==5,"rebuild never changes receipt/change times");}
  try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT street_match_method,street_name,geofence_status FROM fire_119_incident WHERE external_id='outside'")){rs.next();check(rs.getString(1).equals("manual")&&rs.getString(2).equals("人工归属保留")&&rs.getString(3).equals("outside"),"manual attribution preserved but cannot override geometric scope");}
  try(var stmt=db.createStatement()){stmt.execute("UPDATE fire_119_incident SET geofence_version='rollback-test'");stmt.execute("CREATE TRIGGER fixture_fail_rebuild BEFORE UPDATE ON fire_119_incident FOR EACH ROW BEGIN IF OLD.external_id='rescue' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture rebuild rollback'; END IF; END");}
  boolean rebuildFailed=false;try{ingest.rebuildCoordinates(2,Long.MAX_VALUE);}catch(Exception expected){rebuildFailed=true;}check(rebuildFailed,"rebuild database failure surfaced");
  try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT COUNT(*) FROM fire_119_incident WHERE geofence_version='rollback-test'")){rs.next();check(rs.getInt(1)==5,"historical batch rolls back atomically");}
  try(var stmt=db.createStatement()){stmt.execute("DROP TRIGGER fixture_fail_rebuild");}
  while(ingest.rebuildCoordinates(200,Long.MAX_VALUE)>0){}
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
   var sync=new Fire119Synchronizer(cfg,client,ingest,new Fire119PageProcessor(ingest,mapper),ds);sync.syncOnce();check("success".equals(mapper.syncState("fixture_119","320116").get("lastResult")),"complete batch success");long histories=count("fire_119_incident_history");sync.syncOnce();check(count("fire_119_incident_history")==histories,"repeat HTTP batch no duplicated history");
   Object complete=mapper.syncState("fixture_119","320116").get("lastCompletedAt");pageFail.set(true);sync.syncOnce();var state=mapper.syncState("fixture_119","320116");check("failed".equals(state.get("lastResult")),"page failure reported");check(Objects.equals(complete,state.get("lastCompletedAt")),"failed batch does not advance complete time");
   var processor=new Fire119PageProcessor(ingest,mapper);
   hashReads.set(0);lockReads.set(0);pageTouches.set(0);long historyBefore=count("fire_119_incident_history");
   var repeat=processor.process(List.of(fixture("network-1","1",true),fixture("network-2","3",false)),"batch-optimized",Long.MAX_VALUE);
   check(repeat.unchanged()==2&&hashReads.get()==1&&pageTouches.get()==1&&lockReads.get()==0,"unchanged page uses one hash select and one batch update, no row locks");
   check(count("fire_119_incident_history")==historyBefore,"unchanged batch adds no history");
   try(var stmt=db.createStatement()){stmt.execute("UPDATE fire_119_incident SET street_match_method='manual',street_name='人工判定',street_match_status='outside' WHERE external_id='network-2'");}
   processor.process(List.of(fixture("network-2","3",false)),"batch-manual",Long.MAX_VALUE);
   try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT street_match_method,street_name FROM fire_119_incident WHERE external_id='network-2'")){rs.next();check("manual".equals(rs.getString(1))&&"人工判定".equals(rs.getString(2)),"batch metadata preserves manual boundary overrides");}
   lockReads.set(0);
   var changed=fixture("network-1","1",true);changed.put("ajztmc","返队");
   var changedResult=processor.process(List.of(changed),"batch-changed",Long.MAX_VALUE);
   check(changedResult.changed()==1&&lockReads.get()==1&&count("fire_119_incident_history")==historyBefore+1,"changed item retains transactional revision and history");
   Map<String,Object> stale=ingest.prepare(changed,"batch-stale");
   try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT incident_id FROM fire_119_incident WHERE external_id='network-1'")){rs.next();stale.put("incident_id",rs.getLong(1));}
   stale.put("payload_hash","0".repeat(64));boolean conflict=false;try{ingest.touchPage(List.of(stale));}catch(org.springframework.dao.ConcurrencyFailureException e){conflict=true;}
   check(conflict,"stale batch hash cannot overwrite newer row metadata");
   cfg.setQuickPages(1);int callsBefore=calls.get();sync.syncOnce(false);var quickState=mapper.syncState("fixture_119","320116");
   check(calls.get()==callsBefore+1,"quick run requests only configured first page");
   check("success".equals(quickState.get("lastQuickResult")),"quick completion saved independently");
   check("failed".equals(quickState.get("lastResult"))&&Objects.equals(complete,quickState.get("lastCompletedAt")),"quick success cannot erase failed full result or advance full completion");
   check(quickState.get("lastQuickCompletedAt")!=null&&quickState.get("lastNewIncidentAt")!=null,"quick and discovery timestamps available");
   cfg.setQuickPages(2);Object quickComplete=quickState.get("lastQuickCompletedAt");sync.syncOnce(false);quickState=mapper.syncState("fixture_119","320116");
   check("failed".equals(quickState.get("lastQuickResult"))&&Objects.equals(quickComplete,quickState.get("lastQuickCompletedAt")),"failed quick run preserves prior successful quick timestamp");
   check(sync.runtimeStatus().get("nextRetryAt")!=null,"failed run exposes backoff deadline");
   check(Fire119Synchronizer.backoffSeconds(1,60,600)==60&&Fire119Synchronizer.backoffSeconds(2,60,600)==120&&Fire119Synchronizer.backoffSeconds(31,60,600)==600,"bounded exponential backoff");
   pageFail.set(false);sync.syncOnce(false);check(sync.runtimeStatus().get("nextRetryAt")==null,"successful recovery clears retry delay");
   // Real worker scheduling: startup full scan, then quick scans, then another full scan.
   cfg.setEnabled(true);cfg.setIntervalSeconds(1);cfg.setFullIntervalSeconds(3);cfg.setQuickPages(1);cfg.setInitialDelaySeconds(0);cfg.setRetryInitialSeconds(2);cfg.setRetryMaxSeconds(4);
   var scheduled=new Fire119Synchronizer(cfg,client,ingest,processor,ds);
   long fullBefore;try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT COUNT(*) FROM fire_119_sync_log WHERE operation='batch'")){rs.next();fullBefore=rs.getLong(1);}
   scheduled.start();long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(9);boolean cadence=false;
   while(System.nanoTime()<until){try(var stmt=db.createStatement();var rs=stmt.executeQuery("SELECT SUM(operation='batch'),SUM(operation='quick-batch') FROM fire_119_sync_log")){rs.next();if(rs.getLong(1)>=fullBefore+2&&rs.getLong(2)>=3){cadence=true;break;}}Thread.sleep(100);}
   scheduled.stop();check(cadence,"actual scheduler runs full then quick then full at separate cadences");
   var recovering=new Fire119Synchronizer(cfg,client,ingest,processor,ds);
   failAcquire.set(true);int beforeDbFailure=calls.get();recovering.syncOnce();
   check(calls.get()==beforeDbFailure&&recovering.runtimeStatus().get("nextRetryAt")!=null,"database transaction acquisition failure starts backoff before external HTTP");
   failAcquire.set(false);recovering.start();Thread.sleep(400);
   check(calls.get()==beforeDbFailure,"scheduler does not retry during database backoff");
   long recoveryUntil=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(6);
   while(System.nanoTime()<recoveryUntil&&(calls.get()==beforeDbFailure||((Number)recovering.runtimeStatus().get("retryFailures")).intValue()!=0||Boolean.TRUE.equals(recovering.runtimeStatus().get("syncRunning"))))Thread.sleep(100);
   recovering.stop();check(calls.get()>beforeDbFailure&&((Number)recovering.runtimeStatus().get("retryFailures")).intValue()==0,"scheduler resumes after backoff and clears it on recovery");
   int beforeStopped=calls.get();sync.stop();sync.syncOnce();check(calls.get()==beforeStopped,"stopping prevents additional sync and shutdown transaction errors");
   // Keyword inclusion is an independent OR rule and works for existing/outside/unlocated rows.
   cfg.setDachangOnly(true);int baseVisible=service.alarms(query()).size();
   var keywordAddress=fixture("keyword-address","1",false);keywordAddress.put("afdz","大厂街道测试地址");ingest.receive(keywordAddress,"keyword-test");
   var keywordDescription=fixture("keyword-description","2",false);keywordDescription.put("ajms","大厂附近救助");ingest.receive(keywordDescription,"keyword-test");
   var keywordMissing=fixture("keyword-missing","3",false);keywordMissing.put("jd",null);keywordMissing.put("wd",null);keywordMissing.put("afdz",null);keywordMissing.put("ajms","大厂无坐标记录");ingest.receive(keywordMissing,"keyword-test");
   var keywordBoth=fixture("keyword-both","1",true);keywordBoth.put("afdz","大厂");ingest.receive(keywordBoth,"keyword-test");
   var noKeyword=fixture("keyword-negative","1",false);noKeyword.put("afdz",null);noKeyword.put("ajms",null);ingest.receive(noKeyword,"keyword-test");
   try(var stmt=db.createStatement()){stmt.execute("UPDATE fire_119_incident SET geofence_version='old' WHERE external_id='keyword-address'");}
   var included=service.alarms(query());check(included.size()==baseVisible+4,"geometry OR address/description keyword includes outside and missing coordinates");
   check(included.stream().filter(x->"keyword-both".equals(x.get("code"))).count()==1,"coordinate and keyword double match is not duplicated");
   check(included.stream().noneMatch(x->"keyword-negative".equals(x.get("code"))),"outside row with null text does not match");
   var keywordRow=included.stream().filter(x->"keyword-address".equals(x.get("code"))).findFirst().orElseThrow();var keywordDetail=query();keywordDetail.setId(String.valueOf(keywordRow.get("id")));check(service.alarms(keywordDetail).size()==1,"keyword rule also applies to detail and old geometry versions");
   check(String.valueOf(keywordRow.get("streetRegion")).endsWith("地区=大厂街道"),"keyword-included row displays Dachang attribution");check(keywordRow.get("lng")==null&&keywordRow.get("lat")==null&&"pending".equals(keywordRow.get("coordinateStatus")),"keyword row with stale coordinate version cannot display an unverified map point");
   check(included.stream().filter(x->"keyword-missing".equals(x.get("code"))).allMatch(x->x.get("lng")==null&&x.get("lat")==null),"unlocated keyword row is visible without fabricated map coordinates");
   var keywordStatus=service.status(query());check(((Number)keywordStatus.get("visibleCount")).intValue()==included.size(),"keyword count and visible list agree");
   check(((Number)keywordStatus.get("insideCount")).intValue()+((Number)keywordStatus.get("outsideCount")).intValue()+((Number)keywordStatus.get("pendingCount")).intValue()==((Number)keywordStatus.get("total")).intValue(),"effective scope counts partition all rows without double counting");
   var keywordDenied=query();keywordDenied.getParams().put("dataScope"," AND a.dept_id=999");check(service.alarms(keywordDenied).isEmpty(),"keyword cannot bypass department scope");
   check(errors.isEmpty(),"fixture handlers successful: "+errors);
   slowBody.set(true);cfg.setRequestTimeoutSeconds(1);long began=System.nanoTime();boolean timedOut=false;
   try{client.page(1);}catch(Exception expected){timedOut=true;}
   check(timedOut && (System.nanoTime()-began)<java.util.concurrent.TimeUnit.SECONDS.toNanos(3),"timeout covers stalled response body and is bounded");
  }finally{server.stop(0);db.close();context.close();}
  System.out.println("FIRE_119_INTEGRATION_OK checks="+checks+" tokenRequests="+tokens.get()+" pageRequests="+calls.get());
 }
}
