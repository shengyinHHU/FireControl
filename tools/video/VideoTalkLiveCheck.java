import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import tools.jackson.databind.ObjectMapper;
import com.ruoyi.system.fire.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
public class VideoTalkLiveCheck {
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[0]);String target=args[1];var json=new ObjectMapper();var report=new LinkedHashMap<String,Object>();
  report.put("deviceCode",target);report.put("audioBytesSent",0);report.put("businessDatabaseAccessed",false);
  String yaml=Files.readString(root.resolve("RuoYi-Vue-master/ruoyi-admin/src/main/resources/application.yml"));var props=new FireVideoProperties();props.setAppId(value(yaml,"app-id"));props.setAppSecret(value(yaml,"app-secret"));props.setEnterpriseUser(value(yaml,"enterprise-user"));
  var user=new LoginUser();user.setUserId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));var client=new FireVideoClient(props,json,new FireVideoMedia());
  try{
   // Direct diagnostic for the device explicitly named by the user; vendor account scope still applies.
   var business=FireVideoClient.class.getDeclaredMethod("business");business.setAccessible(true);Map b=(Map)business.invoke(client);b.put("deviceCode",target);b.put("domain","1");b.put("netType",0);
   var call=FireVideoClient.class.getDeclaredMethod("call",String.class,Map.class);call.setAccessible(true);Map response=(Map)call.invoke(client,"/open/token/vpaas/voice/talkback",b);
   var ticket=FireVideoClient.voiceTicket(response.get("data"));report.put("ticket","success");
   URI gateway=URI.create((String)ticket.get("wss"));report.put("gatewayHost",gateway.getHost());report.put("gatewayPort",gateway.getPort());
   var messages=new CopyOnWriteArrayList<String>();var closeCode=new java.util.concurrent.atomic.AtomicInteger();var wait=new CountDownLatch(1);var startAck=new CountDownLatch(1);
   var http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
   var socket=http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10)).buildAsync(gateway,new WebSocket.Listener(){
    StringBuilder text=new StringBuilder();
    public void onOpen(WebSocket ws){ws.request(1);}
    public CompletionStage<?> onText(WebSocket ws,CharSequence part,boolean last){text.append(part);if(last){String m=text.toString();text.setLength(0);if(messages.size()<20)messages.add(m);try{Map ack=json.readValue(m,Map.class);if("10001".equals(String.valueOf(ack.get("method")))&&"0".equals(String.valueOf(ack.get("code"))))startAck.countDown();}catch(Exception ignored){}wait.countDown();}ws.request(1);return null;}
    public CompletionStage<?> onClose(WebSocket ws,int code,String reason){closeCode.set(code);wait.countDown();return null;}
    public void onError(WebSocket ws,Throwable error){messages.add("SOCKET_ERROR:"+error.getClass().getSimpleName());wait.countDown();}
   }).get(12,TimeUnit.SECONDS);
   report.put("webSocket","connected");
   try{
    socket.sendText(json.writeValueAsString(Map.of("method","10000","sessionId",ticket.get("sessionId"),"token",ticket.get("token"),"level","1")),true).get(3,TimeUnit.SECONDS);
    boolean acknowledged=startAck.await(5,TimeUnit.SECONDS);report.put("startControlSent",true);report.put("startAcknowledged",acknowledged);
    if(args.length>2&&args[2].equals("tone")&&acknowledged){
     int frames=0;long began=System.nanoTime();
     for(int frame=0;frame<75;frame++){
      byte[] data=new byte[320];for(int sample=0;sample<320;sample++){
       double t=(frame*320+sample)/8000.0;double pulse=t%1.0;double gain=pulse>=0.2&&pulse<0.6?Math.min(1,Math.min((pulse-0.2)/0.02,(0.6-pulse)/0.02)):0;
       data[sample]=alaw((int)(1638*gain*Math.sin(2*Math.PI*440*t)));
      }
      socket.sendBinary(java.nio.ByteBuffer.wrap(data),true).get(2,TimeUnit.SECONDS);frames++;report.put("audioBytesSent",frames*320);
      long remaining=began+(frame+1)*40000000L-System.nanoTime();if(remaining>0)TimeUnit.NANOSECONDS.sleep(remaining);
     }
     report.put("audioFramesSent",frames);report.put("encoding","G711A 8000Hz mono");report.put("testAudio","3s synthetic low-volume pulsed tone; no microphone used");
    }
    socket.sendText(json.writeValueAsString(Map.of("method","10006","sessionId",ticket.get("sessionId"))),true).get(3,TimeUnit.SECONDS);report.put("stopControlSent",true);
    Thread.sleep(1000);
   }finally{if(!Boolean.TRUE.equals(report.get("stopControlSent")))try{socket.sendText(json.writeValueAsString(Map.of("method","10006","sessionId",ticket.get("sessionId"))),true).get(2,TimeUnit.SECONDS);report.put("stopControlSent",true);}catch(Exception ignored){}try{socket.sendClose(WebSocket.NORMAL_CLOSURE,"done").get(2,TimeUnit.SECONDS);}catch(Exception ignored){socket.abort();}}
   List<String> clean=new ArrayList<>();for(String m:messages){m=m.replace((String)ticket.get("token"),"[redacted]");m=m.replaceAll("(?i)(token|sign|authorization|wss)\\s*[:=]\\s*[^, ;]+","$1=[redacted]");clean.add(m.length()>512?m.substring(0,512):m);}
   report.put("gatewayMessages",clean);report.put("closeCode",closeCode.get());
  }catch(Exception failure){Throwable e=failure;while(e.getCause()!=null)e=e.getCause();report.put("failureType",e.getClass().getSimpleName());String message=e.getMessage();if(message!=null&&!message.contains("wss://")&&!message.contains("token"))report.put("failure",message);}
  Files.writeString(root.resolve(args.length>2&&args[2].equals("tone")?"docs/verification/video-talkback-live-audio.json":"docs/verification/video-talkback-live-handshake.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(report));System.out.println(json.writeValueAsString(report));
 }
 static byte alaw(int input){int pcm=input>>3,mask;if(pcm>=0)mask=213;else{mask=85;pcm=-pcm-1;}int[] end={31,63,127,255,511,1023,2047,4095};int segment=0;while(segment<8&&pcm>end[segment])segment++;if(segment>=8)return(byte)(127^mask);int value=segment<<4;value|=segment<2?(pcm>>1)&15:(pcm>>segment)&15;return(byte)(value^mask);}
 static String value(String yaml,String key){var m=java.util.regex.Pattern.compile("(?m)^    "+key+": '([^']+)'$").matcher(yaml);if(!m.find())throw new IllegalArgumentException("Missing video configuration");return m.group(1);}
}
