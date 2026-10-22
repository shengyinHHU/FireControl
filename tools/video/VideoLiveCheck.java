import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import tools.jackson.databind.ObjectMapper;
import com.ruoyi.system.fire.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
public class VideoLiveCheck {
 static String value(String y,String key){var m=Pattern.compile("(?m)^    "+key+": '([^']+)'$").matcher(y);if(!m.find())throw new IllegalArgumentException("Missing config "+key);return m.group(1);}
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args[0]);String y=Files.readString(root.resolve("RuoYi-Vue-master/ruoyi-admin/src/main/resources/application.yml"));
  var p=new FireVideoProperties();p.setAppId(value(y,"app-id"));p.setAppSecret(value(y,"app-secret"));p.setEnterpriseUser(value(y,"enterprise-user"));
  var login=new LoginUser();login.setUserId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
  var json=new ObjectMapper();var media=new FireVideoMedia();var client=new FireVideoClient(p,json,media);
  var out=root.resolve("tools/video/runtime");Files.createDirectories(out);
  Object regions=client.regions(null);Files.writeString(out.resolve("regions.json"),json.writeValueAsString(regions));
  Map page=(Map)client.devices(null,-1,2);Files.writeString(out.resolve("devices.json"),json.writeValueAsString(page));
  List rows=(List)page.get("list");if(rows.isEmpty())throw new IllegalStateException("No devices");
  String code=String.valueOf(((Map)rows.get(0)).get("deviceCode"));
  Map preview=(Map)client.preview(code,"hls");Files.writeString(out.resolve("preview.json"),json.writeValueAsString(preview));
  String mediaUrl=String.valueOf(preview.get("url"));String session=mediaUrl.split("/")[4];
  var manifest=media.read(session,0);Files.write(out.resolve("manifest.m3u8"),manifest.body());
  for(String line:new String(manifest.body(),java.nio.charset.StandardCharsets.UTF_8).split("\r?\n")) {
   if(line.startsWith("./")){int id=Integer.parseInt(line.substring(2));var segment=media.read(session,id);Files.write(out.resolve("media-"+id+".bin"),segment.body());}
  }
  System.out.println("PASS: authenticated media relay manifest and segments");
  System.out.println("LIVE_OK: regions="+((List)regions).size()+", devices="+rows.size()+", HLS preview decrypted");
  login.setUserId(999999L);
  try{media.read(session,0);throw new AssertionError("Media user binding bypass");}catch(com.ruoyi.common.exception.ServiceException expected){System.out.println("PASS: foreign user cannot read media session");}
  try{client.regions(null);throw new AssertionError("Account scope bypass");}catch(com.ruoyi.common.exception.ServiceException expected){System.out.println("PASS: unconfigured user denied before vendor access");}
 }
}
