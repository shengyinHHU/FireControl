import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.sql.DataSource;
import com.sun.net.httpserver.HttpServer;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.*;
import org.mybatis.spring.*;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.*;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.config.*;
import com.ruoyi.framework.config.properties.PermitAllUrlProperties;
import com.ruoyi.framework.security.filter.JwtAuthenticationTokenFilter;
import com.ruoyi.framework.security.handle.*;
import com.ruoyi.framework.web.service.*;
import com.ruoyi.framework.web.exception.GlobalExceptionHandler;
import com.ruoyi.system.fire.sms.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.*;
import com.ruoyi.web.controller.fire.*;
import com.ruoyi.web.controller.system.SysLoginController;

public class SmsOpenIntegrationCheck {
    static DataSource data; static Path root; static int redisPort;
    static FireSmsProperties sms=new FireSmsProperties(); static FireSmsOpenProperties api=new FireSmsOpenProperties();
    static final tools.jackson.databind.ObjectMapper JSON=new tools.jackson.databind.ObjectMapper();
    static final AtomicInteger calls=new AtomicInteger(),audits=new AtomicInteger();
    static final AtomicReference<String> mode=new AtomicReference<>("success");
    static final AtomicReference<Map<?,?>> captured=new AtomicReference<>();
    static final AtomicReference<CountDownLatch> entered=new AtomicReference<>(),release=new AtomicReference<>();
    static HttpServer platform; static String base; static int checks;
    static final String PASSWORD="Test-Sms-827164!";
    static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    static final List<String> labels=new ArrayList<>();
    static void check(boolean pass,String label){if(!pass)throw new AssertionError(label);checks++;labels.add(label);}
    interface Invoke{Object run(String method,Object[] args)throws Throwable;}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,Invoke f){return(T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
        if(m.getName().equals("toString"))return "test-proxy-"+type.getSimpleName();
        if(m.getName().equals("hashCode"))return System.identityHashCode(p);
        if(m.getName().equals("equals"))return p==a[0];
        return f.run(m.getName(),a);
    });}
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class,RedisConfig.class,RedisCache.class,SpringUtils.class,SysLoginService.class,TokenService.class,
        SysPasswordService.class,SysPermissionService.class,UserDetailsServiceImpl.class,SmsOpenAccess.class,
        SmsOpenLoginLimiter.class,SmsOpenTokenService.class,JwtAuthenticationTokenFilter.class,
        AuthenticationEntryPointImpl.class,LogoutSuccessHandlerImpl.class,PermitAllUrlProperties.class,
        FireSmsOpenController.class,FireSmsOpenErrors.class,SysLoginController.class,GlobalExceptionHandler.class})
    static class Config {
        @Bean static org.springframework.context.support.PropertySourcesPlaceholderConfigurer placeholders(){return new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();}
        @Bean DataSource source(){return data;}
        @Bean FireSmsProperties sms(){return sms;}
        @Bean FireSmsOpenProperties api(){return api;}
        @Bean LettuceConnectionFactory redisConnectionFactory(){return new LettuceConnectionFactory("127.0.0.1",redisPort);}
        @Bean StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory f){return new StringRedisTemplate(f);}
        @Bean org.springframework.cache.CacheManager cacheManager(){return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();}
        @Bean CorsFilter corsFilter(){return new CorsFilter(new UrlBasedCorsConfigurationSource());}
        @Bean ResourceBundleMessageSource messageSource(){var s=new ResourceBundleMessageSource();s.setBasename("i18n/messages");s.setDefaultEncoding("UTF-8");return s;}
        @Bean(destroyMethod="shutdownNow") ScheduledExecutorService scheduledExecutorService(){return Executors.newScheduledThreadPool(1);}
        @Bean ISysLogininforService loginAudit(){return proxy(ISysLogininforService.class,(m,a)->{audits.incrementAndGet();return 1;});}
        @Bean ISysConfigService configs(){return proxy(ISysConfigService.class,(m,a)->switch(m){case "selectCaptchaEnabled"->true;case "selectConfigByKey"->"";default->throw new UnsupportedOperationException(m);});}
        @Bean SqlSessionTemplate session()throws Exception{
            var f=new SqlSessionFactoryBean();f.setDataSource(data);f.setTypeAliasesPackage("com.ruoyi.common.core.domain.entity,com.ruoyi.system.domain");
            f.setMapperLocations(java.util.stream.Stream.of("SysUserMapper","SysRoleMapper","SysMenuMapper").map(n->new FileSystemResource(root.resolve("RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/system/"+n+".xml"))).toArray(FileSystemResource[]::new));
            return new SqlSessionTemplate(f.getObject());
        }
        @Bean ISysUserService users(SqlSessionTemplate s){var m=s.getMapper(SysUserMapper.class);return proxy(ISysUserService.class,(n,a)->switch(n){
            case "selectUserByUserName"->m.selectUserByUserName((String)a[0]);case "selectUserById"->m.selectUserById((Long)a[0]);
            case "updateLoginInfo"->{m.updateLoginInfo((Long)a[0],(String)a[1],(Date)a[2]);yield null;}default->throw new UnsupportedOperationException(n);});}
        @Bean ISysRoleService roles(SqlSessionTemplate s){var m=s.getMapper(SysRoleMapper.class);return proxy(ISysRoleService.class,(n,a)->switch(n){case "selectRoleById"->m.selectRoleById((Long)a[0]);default->throw new UnsupportedOperationException(n);});}
        @Bean ISysMenuService menus(SqlSessionTemplate s){var m=s.getMapper(SysMenuMapper.class);return proxy(ISysMenuService.class,(n,a)->switch(n){case "selectMenuPermsByRoleId"->new HashSet<>(m.selectMenuPermsByRoleId((Long)a[0]));default->throw new UnsupportedOperationException(n);});}
        @Bean FireSmsClient client(){return new FireSmsClient(sms,JSON){protected URI endpoint(){return URI.create("http://127.0.0.1:"+platform.getAddress().getPort()+"/send");}};}
        @Bean FireSmsOpenService service(FireSmsClient c){return new FireSmsOpenService(data,sms,api,c);}
    }
    record Reply(int status,Map<String,Object> body,HttpHeaders headers){}
    @SuppressWarnings("unchecked") static Reply call(String method,String path,String token,Object body)throws Exception{
        var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(java.time.Duration.ofSeconds(45)).header("User-Agent","SMS-isolated-test");
        if(token!=null)request.header("Authorization","Bearer "+token);
        if(body!=null)request.header("Content-Type","application/json");
        request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body instanceof String s?s:JSON.writeValueAsString(body)));
        var response=HTTP.send(request.build(),HttpResponse.BodyHandlers.ofString());
        Map<String,Object> parsed;
        try{parsed=JSON.readValue(response.body(),Map.class);}catch(Exception e){throw new AssertionError("Non-JSON "+response.statusCode()+" "+response.body());}
        return new Reply(response.statusCode(),parsed,response.headers());
    }
    static String token(String user)throws Exception{var r=call("POST","/fire/sms/open/token",null,Map.of("username",user,"password",PASSWORD));if(r.status!=200)throw new AssertionError("Login failed: "+r);return (String)r.body.get("token");}
    static Map<String,Object> payload(){return new LinkedHashMap<>(Map.of("requestId",UUID.randomUUID().toString(),"mobiles",List.of("13800000001"),"content","测试 + 中文&=","bizId","biz-1"));}
    @SuppressWarnings("unchecked") static Map<String,Object> result(Reply r){return (Map<String,Object>)r.body.get("data");}
    static Reply send(String t,Map<String,Object> p)throws Exception{return call("POST","/fire/sms/open/send",t,p);}
    static int unsupportedContentType(String token,String type)throws Exception{
        var request=HttpRequest.newBuilder(URI.create(base+"/fire/sms/open/send")).header("Authorization","Bearer "+token).header("Content-Type",type).POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        return HTTP.send(request,HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    public static void main(String[] args)throws Exception{
        root=Path.of(args[2]);redisPort=Integer.parseInt(args[1]);data=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8","root","");var db=new JdbcTemplate(data);
        new RuoYiConfig().setAddressEnabled(false);
        sms.setEnabled(true);sms.setAuthCode("dummy");sms.setAuthKey("dummy-key");sms.setSignature("【测试单位】");api.setEnabled(true);api.setLoginLimit(1000);
        long role=db.queryForObject("SELECT role_id FROM sys_role WHERE role_key='sms_api_client'",Long.class);
        db.update("INSERT INTO sys_role(role_id,role_name,role_key,role_sort,status,del_flag) VALUES(1,'管理员','admin',1,'0','0'),(2,'普通角色','ordinary',2,'0','0')");
        for(int i=1;i<=4;i++){db.update("INSERT INTO sys_user(user_id,dept_id,user_name,nick_name,password,status,del_flag)VALUES(?,100,?,?,?,'0','0')",i,i==1?"admin":i==4?"ordinary":"sms"+i,"测试账号",SecurityUtils.encryptPassword(PASSWORD));db.update("INSERT INTO sys_user_role(user_id,role_id)VALUES(?,?)",i,i==1?1:i==4?2:role);}
        platform=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);platform.setExecutor(Executors.newCachedThreadPool());platform.createContext("/send",x->{try{
            calls.incrementAndGet();Map<?,?> p=JSON.readValue(x.getRequestBody().readAllBytes(),Map.class);captured.set(p);
            if(entered.get()!=null){entered.get().countDown();release.get().await(10,TimeUnit.SECONDS);}
            String response=mode.get().equals("malformed")?"{broken":mode.get().equals("failed")?"{\"code\":-2,\"text\":\"测试拒绝\"}":JSON.writeValueAsString(Map.of("code",1,"text","success","smsid","fixture","phones",mode.get().equals("partial")?"13800000001":p.get("mobiles")));
            byte[] b=response.getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(200,b.length);x.getResponseBody().write(b);
        }catch(Exception e){throw new RuntimeException(e);}finally{x.close();}});platform.start();
        var tomcat=new Tomcat();tomcat.setBaseDir(Files.createTempDirectory("sms-http-").toString());tomcat.setPort(0);tomcat.getConnector().setProperty("address","127.0.0.1");
        var ctx=tomcat.addContext("",Files.createTempDirectory("sms-web-").toString());var spring=new AnnotationConfigWebApplicationContext();spring.setServletContext(ctx.getServletContext());
        spring.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fixture",Map.of("token.header","Authorization","token.secret",Base64.getEncoder().encodeToString(new byte[64]),"token.expireTime","30","user.password.maxRetryCount","5","user.password.lockTime","10")));
        spring.register(Config.class);
        try{
            spring.refresh();Tomcat.addServlet(ctx,"dispatcher",new DispatcherServlet(spring)).setLoadOnStartup(1);ctx.addServletMappingDecoded("/","dispatcher");
            var requestContext=new FilterDef();requestContext.setFilterName("requestContext");requestContext.setFilter(new RequestContextFilter());ctx.addFilterDef(requestContext);var contextMap=new FilterMap();contextMap.setFilterName("requestContext");contextMap.addURLPattern("/*");ctx.addFilterMap(contextMap);
            var fd=new FilterDef();fd.setFilterName("security");fd.setFilter(new DelegatingFilterProxy("springSecurityFilterChain",spring));ctx.addFilterDef(fd);var fm=new FilterMap();fm.setFilterName("security");fm.addURLPattern("/*");ctx.addFilterMap(fm);
            tomcat.start();base="http://127.0.0.1:"+tomcat.getConnector().getLocalPort();
            check(call("POST","/fire/sms/open/send",null,payload()).status==401,"actual JWT filter rejects missing token with HTTP 401");
            check(call("POST","/fire/sms/open/send","invalid",payload()).status==401,"invalid JWT rejected");
            for(String user:List.of("admin","ordinary","missing"))check(call("POST","/fire/sms/open/token",null,Map.of("username",user,"password",PASSWORD)).status==401,"no-captcha login refuses "+user);
            var normal=call("POST","/login",null,Map.of("username","sms2","password",PASSWORD));check(!normal.body.containsKey("token"),"ordinary login still enforces captcha");
            String t=token("sms2"),other=token("sms3");check(t!=null&&t.split("\\.").length==3,"real BCrypt authentication returns signed JWT");
            check(unsupportedContentType(t,"Content-Type")==415,"header name mistakenly used as its value returns HTTP 415");
            check(unsupportedContentType(t,"text/plain")==415,"Postman raw text instead of JSON returns HTTP 415");
            var cache=spring.getBean(RedisCache.class);cache.setCacheObject(CacheConstants.CAPTCHA_CODE_KEY+"ordinary-path","8271");
            var standard=call("POST","/login",null,Map.of("username","sms2","password",PASSWORD,"uuid","ordinary-path","code","8271"));
            check(standard.body.get("token") instanceof String,"existing captcha login still works");
            check(send((String)standard.body.get("token"),payload()).status==401,"ordinary login token cannot bypass dedicated service login");
            var p=payload();var r=send(t,p);check(r.status==200&&result(r).get("taskStatus").equals("submitted"),"JWT and production Redis credential stamp round trip through actual HTTP");
            check(result(r).get("content").equals("【测试单位】测试 + 中文&="),"server adds fixed signature to raw body");
            check(result(r).get("mobiles") instanceof List&&result(r).get("acceptedPhones") instanceof List,"API phones represented as arrays");
            check(!captured.get().containsKey("auth_key")&&captured.get().get("sign")!=null,"provider signing remains server side");
            int count=calls.get();check(send(t,p).status==200&&calls.get()==count,"same request ID never resends");
            var changed=new LinkedHashMap<>(p);changed.put("content","different");check(send(t,changed).status==409,"ID payload mismatch is HTTP 409");
            check(call("GET","/fire/sms/open/requests/"+p.get("requestId"),other,null).status==404,"foreign account cannot read record");
            var q=payload();q.put("mobiles",List.of("13900000002","13800000001"));check(send(t,q).status==200,"batch accepted");count=calls.get();q.put("mobiles",List.of("13800000001","13900000002"));check(send(t,q).status==200&&calls.get()==count,"recipient order has stable idempotence");
            for(var invalid:List.of(Map.of("mobiles",List.of("02512345678")),Map.of("mobiles",List.of("13800000001","13800000001")),Map.of("content","【测试单位】already prefixed"),Map.of("content","x".repeat(1001)),Map.of("spid","injected"),Map.of("requestId","invalid"))){var bad=payload();bad.putAll(invalid);check(send(t,bad).status==400,"bad input rejected: "+invalid.keySet());}
            check(call("POST","/fire/sms/open/send",t,"{broken").status==400,"malformed JSON is HTTP 400");
            var many=payload();many.put("mobiles",java.util.stream.IntStream.range(0,11).mapToObj(i->"138000000"+String.format("%02d",i)).toList());check(send(t,many).status==400,"global maximum ten recipients enforced");
            mode.set("partial");var partial=payload();partial.put("mobiles",List.of("13800000001","13900000002"));var partialReply=result(send(t,partial));check(partialReply.get("taskStatus").equals("partial")&&partialReply.get("acceptedPhones").equals(List.of("13800000001")),"partial acceptance preserves actual accepted recipients");mode.set("success");
            db.update("DELETE FROM fire_sms_open_quota WHERE user_id=2");api.setMinuteLimit(1);api.setDailyLimit(50);
            var first=payload();check(send(t,first).status==200,"quota reservation accepted");var limited=send(t,payload());check(limited.status==429&&limited.headers.firstValue("Retry-After").isPresent(),"minute quota and Retry-After enforced");check(send(t,first).status==200,"idempotent retry bypasses spent quota");
            db.update("DELETE FROM fire_sms_open_quota WHERE user_id=2");api.setMinuteLimit(30);api.setDailyLimit(1);check(send(t,payload()).status==200&&send(t,payload()).status==429,"daily quota enforced separately");
            db.update("DELETE FROM fire_sms_open_quota WHERE user_id=2");var racePool=Executors.newFixedThreadPool(5);var startRace=new CountDownLatch(1);
            try{int beforeRace=calls.get();var attempts=new ArrayList<Future<Reply>>();for(int i=0;i<5;i++)attempts.add(racePool.submit(()->{startRace.await();return send(t,payload());}));startRace.countDown();int ok=0,rejected=0;for(var f:attempts){var status=f.get().status;if(status==200)ok++;else if(status==429)rejected++;}check(ok==1&&rejected==4&&calls.get()==beforeRace+1,"concurrent quota reservations cannot overspend across separate requests");}finally{startRace.countDown();racePool.shutdownNow();}
            api.setDailyLimit(500);db.update("DELETE FROM fire_sms_open_quota WHERE user_id=2");
            mode.set("malformed");var unknown=payload();check(result(send(t,unknown)).get("taskStatus").equals("unknown"),"unparseable provider response stays unknown");count=calls.get();check(send(t,unknown).status==200&&calls.get()==count,"unknown result cannot trigger resend");
            mode.set("failed");check(result(send(t,payload())).get("taskStatus").equals("failed"),"provider rejection returned as task failure");mode.set("success");check(db.queryForObject("SELECT day_count FROM fire_sms_open_quota WHERE user_id=2",Integer.class)==2,"unknown and failed tasks consume reserved recipients");
            entered.set(new CountDownLatch(1));release.set(new CountDownLatch(1));var concurrent=payload();var pool=Executors.newFixedThreadPool(5);
            try{count=calls.get();var f=pool.submit(()->send(t,concurrent));check(entered.get().await(10,TimeUnit.SECONDS),"first concurrent dispatch entered");check(result(send(t,concurrent)).get("taskStatus").equals("pending"),"concurrent duplicate sees durable pending");release.get().countDown();check(f.get().status==200&&calls.get()==count+1,"concurrent duplicate dispatches once");}finally{release.get().countDown();entered.set(null);release.set(null);pool.shutdownNow();}
            db.execute("CREATE TRIGGER open_fixture_failure BEFORE UPDATE ON fire_sms_record FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture DB failure'");var lost=payload();check(send(t,lost).status==500,"post-dispatch database failure returns ambiguous HTTP 500");db.execute("DROP TRIGGER open_fixture_failure");count=calls.get();check(send(t,lost).status==200&&calls.get()==count,"database update failure cannot cause duplicate send");
            db.update("UPDATE fire_sms_record SET create_time=DATE_SUB(NOW(),INTERVAL 61 SECOND)WHERE request_id=?",lost.get("requestId"));check(result(call("GET","/fire/sms/open/requests/"+lost.get("requestId"),t,null)).get("taskStatus").equals("unknown"),"stale pending exposed as unknown");
            db.update("UPDATE sys_user SET status='1' WHERE user_id=2");check(send(t,payload()).status==403,"user disable revokes token immediately");db.update("UPDATE sys_user SET status='0' WHERE user_id=2");check(send(t,payload()).status==401,"revoked Redis token stays invalid after reenable");
            String reset=token("sms2");db.update("UPDATE sys_user SET password=? WHERE user_id=2",SecurityUtils.encryptPassword(PASSWORD));check(send(reset,payload()).status==401,"password reset revokes already issued token");
            String roleToken=token("sms2");db.update("UPDATE sys_role SET status='1' WHERE role_id=?",role);check(send(roleToken,payload()).status==403,"role disable rechecked per request");db.update("UPDATE sys_role SET status='0' WHERE role_id=?",role);
            String extra=token("sms2");db.update("INSERT INTO sys_user_role(user_id,role_id)VALUES(2,2)");check(send(extra,payload()).status==403,"additional active role denied");db.update("DELETE FROM sys_user_role WHERE user_id=2 AND role_id=2");
            String perm=token("sms2");db.update("INSERT INTO sys_role_menu(role_id,menu_id) SELECT ?,menu_id FROM sys_menu WHERE perms='fire:sms:list'",role);check(send(perm,payload()).status==403,"overprivileged SMS role denied");db.update("DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE rm.role_id=? AND m.perms='fire:sms:list'",role);
            for(int i=0;i<5;i++)check(call("POST","/fire/sms/open/token",null,Map.of("username","sms3","password","wrong-password")).status==401,"wrong password attempt "+(i+1));check(call("POST","/fire/sms/open/token",null,Map.of("username","sms3","password",PASSWORD)).status==401,"actual RuoYi Redis password lockout retained");cache.deleteObject(CacheConstants.PWD_ERR_CNT_KEY+"sms3");
            String end=token("sms2");api.setAllowedIps(List.of("192.0.2.0/24"));check(send(end,payload()).status==403,"optional direct peer IP rule enforced");api.setAllowedIps(List.of());api.setEnabled(false);check(send(end,payload()).status==503,"disabled API returns HTTP 503");api.setEnabled(true);
            var weak=new SmsOpenAccess(api,spring.getBean(ISysUserService.class),spring.getBean(ISysRoleService.class),spring.getBean(SysPermissionService.class),spring.getBean(TokenService.class),"abcdefghijklmnopqrstuvwxyz");try{weak.deployment(null);throw new AssertionError("weak secret accepted");}catch(FireSmsOpenException expected){check(expected.getCode()==503,"legacy weak JWT secret cannot enable open API");}
            check(call("POST","/logout",end,null).status==200&&send(end,payload()).status==401,"existing logout invalidates dedicated token");
            var strings=spring.getBean(StringRedisTemplate.class);strings.delete(strings.keys("sms_open:login:*"));api.setLoginLimit(1);check(call("POST","/fire/sms/open/token",null,Map.of("username","missing","password",PASSWORD)).status==401,"first rate limited login checked");check(call("POST","/fire/sms/open/token",null,Map.of("username","missing","password",PASSWORD)).status==429,"real Redis Lua login throttle enforced");api.setLoginLimit(1000);
            check(audits.get()>0,"existing asynchronous login audit used");
            System.out.println(JSON.writeValueAsString(Map.of("passed",checks,"checks",labels,"vendorCallsToLoopbackOnly",calls.get())));
        }finally{spring.close();tomcat.stop();tomcat.destroy();platform.stop(0);((ExecutorService)platform.getExecutor()).shutdownNow();}
    }
}
