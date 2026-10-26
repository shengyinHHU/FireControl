package com.ruoyi.system.fire;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
/** Authenticated, user-bound, short-lived media relay. No client-supplied upstream URLs. */
@Service
public class FireVideoMedia {
 public record Payload(byte[] body,String contentType) {}
 private static final class Session {
  final long userId,expires=System.currentTimeMillis()+600000;
  final List<URI> resources=new ArrayList<>();
  Session(long user,URI uri){userId=user;resources.add(uri);}
  synchronized int add(URI uri){validate(uri);int existing=resources.indexOf(uri);if(existing>=0)return existing;if(resources.size()>=2000)throw new ServiceException("播放会话资源已满，请重新连接");resources.add(uri);return resources.size()-1;}
  synchronized URI get(int id){if(id<0||id>=resources.size())throw new ServiceException("无效媒体资源",404);return resources.get(id);}
 }
 private final Map<String,Session> sessions=new ConcurrentHashMap<>();
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
 private static void validate(URI uri){String host=uri.getHost();if(!"https".equals(uri.getScheme())||host==null||!host.endsWith(".ctyunxs.cn")||uri.getUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443))throw new ServiceException("不支持的媒体服务地址");}
 public String open(String url){
  URI uri=URI.create(url);validate(uri);long now=System.currentTimeMillis();sessions.entrySet().removeIf(e->e.getValue().expires<now);
  if(sessions.size()>=200)throw new ServiceException("视频连接过多，请稍后重试");
  String key=UUID.randomUUID().toString();sessions.put(key,new Session(SecurityUtils.getUserId(),uri));return "/fire/video/media/"+key+"/0";
 }
 // FLV is continuous: stream chunks instead of buffering the whole response.
 private static final class FlvSession {
  final long userId=SecurityUtils.getUserId(),expires=System.currentTimeMillis()+600000;
  final URI uri; boolean claimed,closed; java.net.HttpURLConnection connection;
  FlvSession(URI uri){this.uri=uri;}
  synchronized void close(){closed=true;if(connection!=null)connection.disconnect();}
 }
 private final Map<String,FlvSession> flvSessions=new ConcurrentHashMap<>();
 public String openFlv(String url){
  URI uri=URI.create(url);validate(uri);long now=System.currentTimeMillis();
  flvSessions.entrySet().removeIf(e->{var s=e.getValue();synchronized(s){return !s.claimed&&s.expires<now;}});
  if(flvSessions.size()>=200)throw new ServiceException("视频连接过多，请稍后重试");
  String key=UUID.randomUUID().toString();flvSessions.put(key,new FlvSession(uri));return "/fire/video/flv/"+key;
 }
 private FlvSession flvSession(String key){
  var s=flvSessions.get(key);
  if(s==null)throw new ServiceException("视频会话已过期，请重新连接",410);
  if(s.userId!=SecurityUtils.getUserId())throw new ServiceException("无权访问此视频会话",403);
  return s;
 }
 public void closeFlv(String key){var s=flvSessions.get(key);if(s==null)return;if(s.userId!=SecurityUtils.getUserId())throw new ServiceException("无权访问此视频会话",403);flvSessions.remove(key,s);s.close();}
 public final class FlvStream implements AutoCloseable {
  public final java.io.InputStream input; public final byte[] prefix;
  private final String key; private final FlvSession session;
  private FlvStream(String key,FlvSession session,java.io.InputStream input,byte[] prefix){this.key=key;this.session=session;this.input=input;this.prefix=prefix;}
  public void close(){flvSessions.remove(key,session);session.close();try{input.close();}catch(java.io.IOException ignored){}}
 }
 public FlvStream connectFlv(String key)throws Exception {
  var s=flvSession(key);
  synchronized(s){
   if(s.closed||s.claimed||s.expires<System.currentTimeMillis())throw new ServiceException("视频会话已使用或过期，请重新连接",410);
   s.claimed=true;
   s.connection=(java.net.HttpURLConnection)s.uri.toURL().openConnection();
   s.connection.setUseCaches(false);s.connection.setConnectTimeout(10000);s.connection.setReadTimeout(20000);s.connection.setInstanceFollowRedirects(false);
   s.connection.setRequestProperty("Accept","video/x-flv");
  }
  try{
   int code=s.connection.getResponseCode();if(code!=200)throw new ServiceException("FLV视频连接失败，HTTP "+code);
   var input=s.connection.getInputStream();var prefix=input.readNBytes(13);
   if(prefix.length!=13||prefix[0]!='F'||prefix[1]!='L'||prefix[2]!='V')throw new ServiceException("平台未返回有效FLV视频流");
   synchronized(s){if(s.closed)throw new ServiceException("视频连接已关闭");}
   return new FlvStream(key,s,input,prefix);
  }catch(Exception e){flvSessions.remove(key,s);s.close();throw e;}
 }
 private record RtcSession(long userId,long expires,String stream,URI api) {}
 private final Map<String,RtcSession> rtcSessions=new ConcurrentHashMap<>();
 public String openRtc(String stream){
  URI source=URI.create(stream);
  if(!Set.of("webrtc","rtc").contains(source.getScheme())||!"vnet-voice-gateway.ctseelink.cn".equals(source.getHost())||source.getUserInfo()!=null||(source.getPort()!=-1&&source.getPort()!=443))throw new ServiceException("不支持的RTC服务地址");
  long now=System.currentTimeMillis();rtcSessions.entrySet().removeIf(e->e.getValue().expires()<now);
  if(rtcSessions.size()>=200)throw new ServiceException("视频连接过多，请稍后重试");
  String key=UUID.randomUUID().toString();
  URI api=URI.create("https://"+source.getHost()+"/webrtc-gateway/rtc/v1/play/"+(source.getRawQuery()==null?"":"?"+source.getRawQuery()));
  rtcSessions.put(key,new RtcSession(SecurityUtils.getUserId(),now+600000,stream,api));return "/fire/video/rtc/"+key;
 }
 public Map<String,Object> signalRtc(String key,String sdp,tools.jackson.databind.ObjectMapper json)throws Exception {
  RtcSession s=rtcSessions.get(key);
  if(s==null||s.expires()<System.currentTimeMillis()){rtcSessions.remove(key);throw new ServiceException("视频会话已过期，请重新连接",410);}
  if(s.userId()!=SecurityUtils.getUserId())throw new ServiceException("无权访问此视频会话",403);
  if(sdp==null||sdp.isBlank()||sdp.length()>131072)throw new ServiceException("无效RTC协商数据");
  Map<String,Object> body=new LinkedHashMap<>();body.put("api",s.api().toString());body.put("tid",UUID.randomUUID().toString().substring(0,7));body.put("streamurl",s.stream());body.put("clientip",null);body.put("sdp",sdp);
  var req=HttpRequest.newBuilder(s.api()).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
  var response=http.send(req,HttpResponse.BodyHandlers.ofInputStream());byte[] bytes;
  try(var input=response.body()){if(response.statusCode()!=200)throw new ServiceException("RTC网关连接失败，HTTP "+response.statusCode());bytes=input.readNBytes(262145);}
  if(bytes.length>262144)throw new ServiceException("RTC网关响应过大");
  Map<String,Object> answer=json.readValue(bytes,Map.class);
  if(!"0".equals(String.valueOf(answer.get("code")))||!(answer.get("sdp") instanceof String remote)||remote.isBlank())throw new ServiceException("RTC网关协商失败，代码 "+answer.get("code"));
  return Map.of("code",0,"sdp",answer.get("sdp"));
 }
 private String local(String key,Session s,URI base,String relative){return "/fire/video/media/"+key+"/"+s.add(base.resolve(relative));}
 public Payload read(String key,int resource) throws Exception {
  Session s=sessions.get(key);
  if(s==null||s.expires<System.currentTimeMillis()){sessions.remove(key);throw new ServiceException("视频会话已过期，请重新连接",410);}
  if(s.userId!=SecurityUtils.getUserId())throw new ServiceException("无权访问此视频会话",403);
  URI uri=s.get(resource);
  var req=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build();
  var response=http.send(req,HttpResponse.BodyHandlers.ofInputStream());
  byte[] bytes;
  try(var input=response.body()){
   if(response.statusCode()!=200)throw new ServiceException("视频资源暂不可用，HTTP "+response.statusCode());
   bytes=input.readNBytes(16*1024*1024+1);
  }
  if(bytes.length>16*1024*1024)throw new ServiceException("视频分片超过大小限制");
  String type=response.headers().firstValue("Content-Type").orElse("application/octet-stream");
  if(uri.getPath().endsWith(".m3u8")||type.contains("mpegurl")){
   String text=new String(bytes,StandardCharsets.UTF_8);if(!text.startsWith("#EXTM3U"))throw new ServiceException("无效视频播放清单");
   StringBuilder out=new StringBuilder();
   for(String line:text.split("\r?\n")){
    if(!line.isBlank()&&!line.startsWith("#"))line=local(key,s,uri,line.trim());
    else if(line.contains("URI=\"")){
     Matcher m=Pattern.compile("URI=\"([^\"]+)\"").matcher(line);StringBuffer sb=new StringBuffer();
     while(m.find())m.appendReplacement(sb,Matcher.quoteReplacement("URI=\""+local(key,s,uri,m.group(1))+"\""));m.appendTail(sb);line=sb.toString();
    }
    out.append(line).append('\n');
   }
   // Prefix is relative to playlist request and works with /dev-api or production API prefixes.
   String prefix="/fire/video/media/"+key+"/";
   String rewritten=out.toString().replace(prefix, "./");
   return new Payload(rewritten.getBytes(StandardCharsets.UTF_8),"application/vnd.apple.mpegurl");
  }
  return new Payload(bytes,type);
 }
}
