import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.fire.*;
import tools.jackson.databind.ObjectMapper;

/** Explicit maintenance entry point: reads configured credentials in memory, never prints them. */
public class VideoCatalogInitialSync {
 static Map map(Map parent,String key){return (Map)parent.get(key);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[0]);var yaml=new Yaml();
  Map config=yaml.load(Files.readString(root.resolve("RuoYi-Vue-master/ruoyi-admin/src/main/resources/application.yml")));
  Map video=map(map(config,"fire"),"video");
  var properties=new FireVideoProperties();properties.setEnabled(!Boolean.FALSE.equals(video.get("enabled")));properties.setAppId(String.valueOf(video.get("app-id")));properties.setAppSecret(String.valueOf(video.get("app-secret")));properties.setEnterpriseUser(String.valueOf(video.get("enterprise-user")));
  Map datasource=yaml.load(Files.readString(root.resolve("RuoYi-Vue-master/ruoyi-admin/src/main/resources/application-druid.yml")));Map master=map(map(map(map(datasource,"spring"),"datasource"),"druid"),"master");
  var ds=new DriverManagerDataSource(String.valueOf(master.get("url")),String.valueOf(master.get("username")),String.valueOf(master.get("password")));
  var login=new LoginUser();login.setUserId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
  var json=new ObjectMapper();var client=new FireVideoClient(properties,json,new FireVideoMedia());var service=new FireVideoCatalogService(ds,client,json,new DataSourceTransactionManager(ds));
  try{
   service.startSync();var db=new JdbcTemplate(ds);String source=client.sourceId();
   for(int i=0;i<650;i++){
    var state=db.queryForMap("SELECT state,directories,devices,message FROM fire_video_sync_state WHERE source_id=?",source);
    if(!"running".equals(state.get("state"))){
     if(!"complete".equals(state.get("state")))throw new IllegalStateException("Platform sync did not complete; existing catalog and coordinates preserved");
     Path output=root.resolve("outputs/video-catalog");Files.createDirectories(output);
     try(var stream=Files.newOutputStream(output.resolve("监控坐标调查表.xlsx"))){FireVideoCoordinateExcel.write(service.export(null,null,"pending"),stream);}
     var stats=(Map)service.list(null,null,null,1,20).get("summary");var report=Map.of("liveDatabase",true,"liveVendor",true,"directories",state.get("directories"),"devices",state.get("devices"),"summary",stats,"coordinatesInvented",false,"workbook","outputs/video-catalog/监控坐标调查表.xlsx");
     Files.writeString(output.resolve("initial-sync.json"),json.writeValueAsString(report));System.out.println(json.writeValueAsString(report));return;
    }
    if(i%10==0)System.out.println("同步进行中：目录="+state.get("directories")+"，设备="+state.get("devices"));Thread.sleep(1000);
   }
   throw new IllegalStateException("Platform sync timed out; inspect sync state before retrying");
  }finally{service.close();}
 }
}
