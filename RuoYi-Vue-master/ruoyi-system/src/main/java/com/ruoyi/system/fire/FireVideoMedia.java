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
