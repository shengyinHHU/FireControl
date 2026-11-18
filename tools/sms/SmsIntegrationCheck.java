import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.sun.net.httpserver.HttpServer;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.context.request.*;
import com.ruoyi.framework.aspectj.DataScopeAspect;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.fire.sms.*;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.*;
import com.ruoyi.system.service.impl.FirePlaceServiceImpl;
import com.ruoyi.web.controller.fire.FireSmsController;

public class SmsIntegrationCheck {
    static int checks;
    static final Set<String> ALL = Set.of("fire:places:list", "fire:inspections:list", "fire:sms:send", "fire:sms:list");
    static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    interface Run { void run() throws Exception; }
    static void denied(Run action, String label) throws Exception { try { action.run(); throw new AssertionError(label); } catch (ServiceException expected) { checks++; } }
    static void login(long userId, long dept, String scope, Set<String> permissions) {
        var role = new SysRole(); role.setRoleId(2L); role.setStatus("0"); role.setDataScope(scope); role.setPermissions(permissions);
        var user = new SysUser(); user.setUserId(userId); user.setDeptId(dept); user.setUserName("test"+userId); user.setNickName("测试员"+userId); user.setRoles(List.of(role));
        var login = new LoginUser(); login.setUser(user); login.setUserId(userId); login.setDeptId(dept); login.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        RequestContextHolder.setRequestAttributes(new RequestAttributes() {
            final Map<String,Object> data = new HashMap<>();
            public Object getAttribute(String n,int s){return data.get(n);} public void setAttribute(String n,Object v,int s){data.put(n,v);} public void removeAttribute(String n,int s){data.remove(n);} public String[] getAttributeNames(int s){return data.keySet().toArray(String[]::new);} public void registerDestructionCallback(String n,Runnable r,int s){} public Object resolveReference(String n){return null;} public String getSessionId(){return "fixture";} public Object getSessionMutex(){return this;}
        });
    }
    static Map<String,Object> input(String type, String id, String parent, String phone, String body) {
        var value = new LinkedHashMap<String,Object>(); value.put("type", type); value.put("id", id); value.put("parentId", parent);
        value.put("expectedPhone", phone); value.put("signature", "【测试单位】"); value.put("phones", List.of("13800000001")); value.put("body", body); value.put("requestId", UUID.randomUUID().toString()); return value;
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[1]); var ds = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8", "root", ""); var db = new JdbcTemplate(ds);
        var factory = new SqlSessionFactoryBean(); factory.setDataSource(ds);
        factory.setMapperLocations(new FileSystemResource(root.resolve("RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FirePlaceMapper.xml")), new FileSystemResource(root.resolve("RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FireInspectionMapper.xml")), new FileSystemResource(root.resolve("RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FireSmsMapper.xml")));
        var session = new SqlSessionTemplate(factory.getObject());
        var placeImpl = new FirePlaceServiceImpl(); var field = FirePlaceServiceImpl.class.getDeclaredField("placeMapper"); field.setAccessible(true); field.set(placeImpl, session.getMapper(FirePlaceMapper.class));
        var placeProxy = new AspectJProxyFactory(placeImpl); placeProxy.addAspect(new DataScopeAspect()); IFirePlaceService places = placeProxy.getProxy();
        var inspectionProxy = new AspectJProxyFactory(new FireInspectionReadService(session.getMapper(FireInspectionMapper.class))); inspectionProxy.addAspect(new DataScopeAspect()); FireInspectionReadService inspections = inspectionProxy.getProxy();
        var readProxy = new AspectJProxyFactory(new FireSmsReadService(session.getMapper(FireSmsMapper.class))); readProxy.addAspect(new DataScopeAspect()); FireSmsReadService reads = readProxy.getProxy();
        db.update("INSERT INTO fire_place(place_id,name,place_type,dept_id,supervisor_user_id) VALUES(1,'测试场所','industry',10,2),(2,'范围外场所','industry',20,3)");
        db.update("INSERT INTO fire_place_contact(contact_id,place_id,contact_role,contact_name,phone) VALUES(11,1,'principal','测试联系人','13800000001;13900000002'),(12,2,'principal','范围外联系人','13800000001'),(13,1,'landline','座机联系人','025-12345678')");
        db.update("INSERT INTO fire_inspection_post(post_id,post_name,dept_id,supervisor_user_id,contact_name,contact_phone,create_by,update_by,create_time,update_time) VALUES(21,'测试值守岗',10,2,'值守联系人','13800000001','fixture','fixture',NOW(),NOW())");
        var json = new tools.jackson.databind.ObjectMapper(); var config = new FireSmsProperties(); config.setEnabled(true); config.setAuthCode("dummy-code"); config.setAuthKey("dummy-key"); config.setSpid("test-spid"); config.setSignature("【测试单位】");
        var count = new AtomicInteger(); var mode = new AtomicReference<>("success"); var last = new AtomicReference<Map<?,?>>(); var entered = new AtomicReference<CountDownLatch>(); var release = new AtomicReference<CountDownLatch>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.createContext("/send", exchange -> {
            try {
                count.incrementAndGet(); var request = json.readValue(exchange.getRequestBody().readAllBytes(), Map.class); last.set(request);
                if (entered.get() != null) { entered.get().countDown(); release.get().await(10, TimeUnit.SECONDS); }
                String response;
                if (mode.get().equals("failed")) response = "{\"code\":-2,\"text\":\"签名未报备通过\"}";
                else if (mode.get().equals("malformed")) response = "{bad json";
                else response = json.writeValueAsString(Map.of("code", 1, "text", "success", "smsid", "platform-fixture", "phones", mode.get().equals("partial") ? "13800000001" : request.get("mobiles")));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
            } catch (Exception e) { throw new RuntimeException(e); } finally { exchange.close(); }
        }); server.start();
        try {
            var client = new FireSmsClient(config, json) { @Override protected URI endpoint() { return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/send"); } };
            var service = new FireSmsService(ds, places, inspections, config, client); var controller = new FireSmsController(service, reads);
            login(2,10,"5",ALL);
            check(FireSmsService.phones("025-12345678, +8613800000001 / 13900000002;13800000001").equals(List.of("13800000001","13900000002")), "mobile-only normalization and deduplication");
            check(((List<?>)service.recipient("placeContact",11L,1L).get("phones")).size()==2, "scoped real contact preview");
            check(service.recipient("inspectionPost",21L,null).get("name").equals("值守联系人"), "inspection contact resolved on server");
            denied(() -> service.recipient("placeContact",12L,2L), "foreign contact denied by actual DataScope");
            denied(() -> service.recipient("placeContact",11L,2L), "parent/contact mismatch denied");
            denied(() -> service.recipient("personnel",11L,1L), "demo resource cannot send real SMS");
            var payload = input("placeContact","11","1","13800000001;13900000002","请检查 +()!*~ /&=中文");
            var accepted = service.send(payload); check(accepted.get("taskStatus").equals("submitted"), "platform acceptance persisted");
            check(last.get().get("content").equals("%E3%80%90%E6%B5%8B%E8%AF%95%E5%8D%95%E4%BD%8D%E3%80%91%E8%AF%B7%E6%A3%80%E6%9F%A5+%2B%28%29%21*%7E+%2F%26%3D%E4%B8%AD%E6%96%87"), "Java URL encoding exactly once");
            check(!last.get().containsKey("auth_key"), "auth key never transmitted");
            String signing = "dummy-codetest-spid"+last.get().get("smsid")+last.get().get("mobiles")+last.get().get("content")+last.get().get("timestamp")+"dummy-key";
            String expected = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(signing.getBytes(StandardCharsets.UTF_8)));
            check(expected.equals(last.get().get("sign")), "captured outbound signature agrees with protocol order");
            int before=count.get(); check(service.send(payload).get("recordId").equals(accepted.get("recordId")) && count.get()==before, "identical retry returns original result without dispatch");
            var changed=new LinkedHashMap<>(payload); changed.put("body","改过的内容"); denied(() -> service.send(changed), "same key different payload rejected");
            var arbitrary=input("placeContact","11","1","13800000001;13900000002","越权号码"); arbitrary.put("phones",List.of("13700000003")); denied(() -> service.send(arbitrary), "arbitrary recipient injection denied");
            var stale=input("placeContact","11","1","13800000001","旧号码"); denied(() -> service.send(stale), "changed source phone requires new preview");
            var landline=input("placeContact","13","1","025-12345678","座机"); denied(() -> service.send(landline), "landline cannot receive SMS");
            config.setEnabled(false); denied(() -> service.send(input("inspectionPost","21","","13800000001","未启用")), "disabled provider never dispatches"); config.setEnabled(true);
            check(count.get()==before, "all preflight rejection paths have no outbound side effect");
            var invalid=controller.send(stale); check(((Map<?,?>)invalid.get("data")).get("taskStatus").equals("rejected"), "UI receives explicit safe pre-dispatch validation result");
            mode.set("failed"); var failed=service.send(input("inspectionPost","21","","13800000001","签名失败")); check(failed.get("taskStatus").equals("failed") && failed.get("resultText").equals("签名未报备通过"), "provider failure preserved");
            mode.set("malformed"); var unknownPayload=input("inspectionPost","21","","13800000001","未知结果"); var unknown=service.send(unknownPayload); check(unknown.get("taskStatus").equals("unknown"), "malformed response is uncertain rather than safe-to-retry"); before=count.get(); service.send(unknownPayload); check(count.get()==before,"uncertain result never automatically resubmitted");
            mode.set("partial"); var multi=input("placeContact","11","1","13800000001;13900000002","部分受理"); multi.put("phones",List.of("13800000001","13900000002")); check(service.send(multi).get("taskStatus").equals("partial"), "partial acceptance records actual accepted phones");
            mode.set("success"); entered.set(new CountDownLatch(1)); release.set(new CountDownLatch(1)); var concurrent=input("inspectionPost","21","","13800000001","并发提交"); var pool=Executors.newSingleThreadExecutor();
            try {
                int start=count.get(); var future=pool.submit(() -> { login(2,10,"5",ALL); return service.send(concurrent); }); check(entered.get().await(10,TimeUnit.SECONDS), "first dispatch entered");
                check(service.send(concurrent).get("taskStatus").equals("pending"), "concurrent duplicate observes durable pending record");
                release.get().countDown(); check(future.get(10,TimeUnit.SECONDS).get("taskStatus").equals("submitted") && count.get()==start+1, "concurrent requests dispatch exactly once");
            } finally { release.get().countDown(); pool.shutdownNow(); entered.set(null); release.set(null); }
            db.execute("CREATE TRIGGER sms_fixture_failure BEFORE UPDATE ON fire_sms_record FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture DB failure'");
            var persistFailure=input("inspectionPost","21","","13800000001","受理后数据库异常"); try { service.send(persistFailure); throw new AssertionError("update should fail"); } catch(org.springframework.dao.DataAccessException expectedFailure) { checks++; }
            db.execute("DROP TRIGGER sms_fixture_failure"); before=count.get(); check(service.send(persistFailure).get("taskStatus").equals("pending") && count.get()==before,"post-dispatch DB failure cannot cause duplicate send");
            db.update("UPDATE fire_sms_record SET create_time=DATE_SUB(NOW(),INTERVAL 61 SECOND) WHERE request_id=?",persistFailure.get("requestId")); check(service.request(persistFailure.get("requestId").toString()).get("taskStatus").equals("unknown"), "stale pending explicitly becomes uncertain");
            db.update("UPDATE fire_inspection_post SET status='1' WHERE post_id=21"); denied(() -> service.recipient("inspectionPost",21L,null),"disabled post cannot send");
            var query=new FireSmsQuery(); query.getParams().put("dataScope"," OR 1=1"); check(reads.records(query).size()>=6,"owner sees durable real records");
            login(3,20,"5",ALL); query=new FireSmsQuery(); query.getParams().put("dataScope"," OR 1=1"); check(reads.records(query).isEmpty(),"record scope clears injected client SQL"); denied(() -> service.request(payload.get("requestId").toString()),"foreign sender cannot inspect request");
            login(4,10,"3",ALL); check(!reads.records(new FireSmsQuery()).isEmpty(),"department record scope");
            login(2,10,"5",Set.of("fire:sms:send")); denied(() -> service.recipient("placeContact",11L,1L),"SMS permission alone cannot read source contact");
            login(2,10,"5",Set.of("fire:places:list")); denied(() -> service.send(payload),"source read permission alone cannot send SMS");
            check(db.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE perms='fire:sms:send'",Integer.class)==1,"repeat-safe permission migration");
            System.out.println("SMS_INTEGRATION_OK checks="+checks+"; actual MySQL/MyBatis/DataScope, loopback HTTP, concurrency and failure recovery; no business DB or live SMS accessed");
        } finally { server.stop(0); }
    }
}
