import java.nio.file.*;
import java.util.*;
import com.ruoyi.system.fire.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import tools.jackson.databind.ObjectMapper;
public class VideoFlvCheck {
 static String value(String y,String key){var m=java.util.regex.Pattern.compile("(?m)^    "+key+": '([^']+)'$").matcher(y);if(!m.find())throw new IllegalArgumentException("Missing video configuration field");return m.group(1);}
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[0]);String y=Files.readString(root.resolve("RuoYi-Vue-master/ruoyi-admin/src/main/resources/application.yml"));
  var p=new FireVideoProperties();p.setAppId(value(y,"app-id"));p.setAppSecret(value(y,"app-secret"));p.setEnterpriseUser(value(y,"enterprise-user"));
  var user=new LoginUser();user.setUserId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
  var json=new ObjectMapper();var media=new FireVideoMedia();var c=new FireVideoClient(p,json,media);String target="32011601021322001599";boolean found=false;long last=-1;
  for(int i=0;i<5;i++){Map page=(Map)c.devices(null,last,100);for(Object row:(List)page.get("list"))if(target.equals(((Map)row).get("deviceCode"))){found=true;}if(found)break;Object next=page.get("lastId");if(next==null||Long.parseLong(next.toString())<0)break;last=Long.parseLong(next.toString());}
  if(!found)throw new IllegalStateException("Target device absent from current account listing");
  if(args.length>1&&args[1].equals("serve")){serve(c,media,json);return;}
  Map preview=(Map)c.preview(target,"flv");
  if(!Boolean.TRUE.equals(preview.get("requiresAuth"))||!"flv".equals(preview.get("protocol")))throw new AssertionError("FLV preview contract");
  String key=((String)preview.get("url")).substring("/fire/video/flv/".length());
  user.setUserId(999999L);
  try{media.connectFlv(key);throw new AssertionError("Foreign user accepted");}catch(com.ruoyi.common.exception.ServiceException expected){if(!Integer.valueOf(403).equals(expected.getCode()))throw expected;}
  user.setUserId(1L);
  var bytes=new java.io.ByteArrayOutputStream();
  try(var stream=media.connectFlv(key)){
   try{media.connectFlv(key);throw new AssertionError("Session replay accepted");}catch(com.ruoyi.common.exception.ServiceException expected){}
   bytes.write(stream.prefix);long until=System.currentTimeMillis()+12000;byte[] buffer=new byte[32768];
   while(System.currentTimeMillis()<until&&bytes.size()<4*1024*1024){int n=stream.input.read(buffer);if(n<0)break;bytes.write(buffer,0,n);}
  }
  Files.write(root.resolve("tools/video/runtime/flv-sample.flv"),bytes.toByteArray());
  System.out.println("FLV_PASS bytes="+bytes.size()+"; foreign-user denied; single-use enforced; stream closed");
 }
 static void serve(FireVideoClient client,FireVideoMedia media,ObjectMapper json)throws Exception {
  var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",5186),0);
  var pool=java.util.concurrent.Executors.newCachedThreadPool();server.setExecutor(pool);
  var keys=java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
  var controller=new com.ruoyi.web.controller.fire.FireVideoController(client,media);
  server.createContext("/",ex->{
   boolean committed[]=new boolean[1];
   try{
    var login=new LoginUser();login.setUserId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
    ex.getResponseHeaders().set("Access-Control-Allow-Origin","http://127.0.0.1:5177");
    ex.getResponseHeaders().set("Access-Control-Allow-Headers","Authorization");ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,DELETE,OPTIONS");
    if(ex.getRequestMethod().equals("OPTIONS")){ex.sendResponseHeaders(204,-1);return;}
    if(!"Bearer fixture-only".equals(ex.getRequestHeaders().getFirst("Authorization"))){ex.sendResponseHeaders(403,-1);return;}
    if(ex.getRequestURI().getPath().equals("/preview")){
     Map preview=(Map)client.preview("32011601021322001599","flv");keys.add(((String)preview.get("url")).substring("/fire/video/flv/".length()));
     byte[] bytes=json.writeValueAsBytes(Map.of("code",200,"data",preview));ex.getResponseHeaders().set("Content-Type","application/json");ex.sendResponseHeaders(200,bytes.length);ex.getResponseBody().write(bytes);return;
    }
    String key=ex.getRequestURI().getPath().substring("/fire/video/flv/".length());
    if(ex.getRequestMethod().equals("DELETE")){controller.closeFlv(key);keys.remove(key);ex.sendResponseHeaders(200,-1);return;}
    var output=new jakarta.servlet.ServletOutputStream(){
     public boolean isReady(){return true;}public void setWriteListener(jakarta.servlet.WriteListener l){}
     private void begin()throws java.io.IOException{if(!committed[0]){ex.sendResponseHeaders(200,0);committed[0]=true;}}
     public void write(int b)throws java.io.IOException{begin();ex.getResponseBody().write(b);}
     public void write(byte[] b,int off,int n)throws java.io.IOException{begin();ex.getResponseBody().write(b,off,n);}
     public void flush()throws java.io.IOException{begin();ex.getResponseBody().flush();}
    };
    var response=(jakarta.servlet.http.HttpServletResponse)java.lang.reflect.Proxy.newProxyInstance(VideoFlvCheck.class.getClassLoader(),new Class[]{jakarta.servlet.http.HttpServletResponse.class},(proxy,method,values)->{
     switch(method.getName()){
      case "setContentType":ex.getResponseHeaders().set("Content-Type",(String)values[0]);return null;
      case "setHeader":ex.getResponseHeaders().set((String)values[0],(String)values[1]);return null;
      case "getOutputStream":return output;
      case "isCommitted":return committed[0];
      default:throw new UnsupportedOperationException(method.getName());
     }
    });
    controller.flv(key,response);keys.remove(key);
   }catch(Exception failure){if(!committed[0])ex.sendResponseHeaders(502,-1);System.out.println("FLV_RELAY_EVENT "+failure.getClass().getSimpleName());}
   finally{SecurityContextHolder.clearContext();ex.close();}
  });
  server.start();System.out.println("FLV_RELAY_READY loopback-only; auto-stop=60s");
  try{Thread.sleep(60000);}finally{for(String key:keys)media.closeFlv(key);server.stop(0);pool.shutdownNow();}
 }

}
