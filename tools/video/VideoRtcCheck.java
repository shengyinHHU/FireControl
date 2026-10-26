import java.nio.file.*;
import java.util.*;
import com.ruoyi.system.fire.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import tools.jackson.databind.ObjectMapper;
public class VideoRtcCheck {
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[0]);String y=Files.readString(root.resolve("RuoYi-Vue-master/ruoyi-admin/src/main/resources/application.yml"));
  var p=new FireVideoProperties();p.setAppId(VideoLiveCheck.value(y,"app-id"));p.setAppSecret(VideoLiveCheck.value(y,"app-secret"));p.setEnterpriseUser(VideoLiveCheck.value(y,"enterprise-user"));
  var user=new LoginUser();user.setUserId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
  var json=new ObjectMapper();var media=new FireVideoMedia();var c=new FireVideoClient(p,json,media);String target="32011601021322001599";boolean found=false;long last=-1;
  for(int i=0;i<5;i++){Map page=(Map)c.devices(null,last,100);for(Object row:(List)page.get("list"))if(target.equals(((Map)row).get("deviceCode"))){found=true;Files.writeString(root.resolve("tools/video/runtime/rtc-device.json"),json.writeValueAsString(row));}if(found)break;Object next=page.get("lastId");if(next==null||Long.parseLong(next.toString())<0)break;last=Long.parseLong(next.toString());}
  if(!found)throw new IllegalStateException("Target device absent from current account listing");
  Map preview=(Map)c.preview(target,"webrtc");
  if(args.length>1 && args[1].equals("hls")){
   Map hls=(Map)c.preview(target,"hls");String url=(String)hls.get("url");String key=url.split("/")[4];int resource=0;
   for(int depth=0;depth<4;depth++){
    var payload=media.read(key,resource);
    if(!payload.contentType().contains("mpegurl")){Files.write(root.resolve("tools/video/runtime/rtc-target-hls.ts"),payload.body());System.out.println("TARGET_HLS_SEGMENT_OK bytes="+payload.body().length);return;}
    String manifest=new String(payload.body(),java.nio.charset.StandardCharsets.UTF_8);String next=Arrays.stream(manifest.split("\\r?\\n")).filter(line->line.startsWith("./")).findFirst().orElseThrow();resource=Integer.parseInt(next.substring(2));
   }throw new IllegalStateException("Nested playlist limit");
  }
  if(args.length>1){
   var business=FireVideoClient.class.getDeclaredMethod("business");business.setAccessible(true);Map b=(Map)business.invoke(c);b.put("deviceCode",target);b.put("mediaType",args[1].equals("hd")?0:1);b.put("mute",args[1].equals("mute")?1:0);
   var call=FireVideoClient.class.getDeclaredMethod("call",String.class,Map.class);call.setAccessible(true);Map response=(Map)call.invoke(c,"/open/token/vpaas/getDeviceMediaWebrtcUrl",b);
   String text=(String)response.get("data");preview=json.readValue(com.ruoyi.system.fire.video.XXTea.decrypt(text,"UTF-8",com.ruoyi.system.fire.video.ByteFormat.toHex(p.getAppSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8))),Map.class);
  }
  Files.writeString(root.resolve("tools/video/runtime/rtc-preview.json"),json.writeValueAsString(preview));
  System.out.println("RTC_ADDRESS_OK: protocol="+preview.get("protocol")+", signed URL saved locally without logging");
 }
}