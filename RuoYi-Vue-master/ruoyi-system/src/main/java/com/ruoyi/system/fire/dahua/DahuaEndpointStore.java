package com.ruoyi.system.fire.dahua;

import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.fire.FireVideoClient;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.dao.DataAccessException;

@Service
public class DahuaEndpointStore {
 private final JdbcTemplate db; private final FireVideoClient client;private final DahuaProperties properties;
 public DahuaEndpointStore(DataSource ds,FireVideoClient c,DahuaProperties p){db=new JdbcTemplate(ds);client=c;properties=p;}
 public record Endpoint(long cameraId,String host,int port,String credentialRef,boolean enabled,int revision){}
 public record Edit(String host,int port,String credentialRef,boolean enabled,int revision){}
 public long camera(String code){
  if(code==null||code.length()>128)throw new ServiceException("监控设备编码无效");
  var rows=db.queryForList("SELECT camera_id FROM fire_video_camera WHERE source_id=? AND device_code=?",Long.class,client.sourceId(),code);
  if(rows.size()!=1)throw new ServiceException("请先同步监控台账，并选择台账中的设备");return rows.get(0);
 }
 public Endpoint get(long id){
  try{var rows=db.query("SELECT * FROM fire_video_talk_endpoint WHERE camera_id=?",(r,n)->new Endpoint(id,r.getString("host"),r.getInt("sdk_port"),r.getString("credential_ref"),r.getBoolean("enabled"),r.getInt("revision")),id);return rows.isEmpty()?null:rows.get(0);}
  catch(DataAccessException e){throw new ServiceException("对讲连接表未就绪，请导入SQL/fire_video_dahua_talk.sql");}
 }
 public Endpoint save(long id,Edit e,String user){
  if(e==null||!privateIpv4(e.host())||e.port()<1||e.port()>65535||e.credentialRef()==null||!properties.getCredentials().containsKey(e.credentialRef()))throw new ServiceException("请填写有效专网IPv4、端口并选择登录配置");
  Endpoint old=get(id);
  if(old==null){if(e.revision()!=-1)throw new ServiceException("连接配置已变化，请重新加载");
   try{db.update("INSERT INTO fire_video_talk_endpoint(camera_id,host,sdk_port,credential_ref,enabled,create_by,update_by) VALUES(?,?,?,?,?,?,?)",id,e.host(),e.port(),e.credentialRef(),e.enabled(),user,user);}catch(DataAccessException ex){throw new ServiceException("保存失败：该摄像机地址可能已被绑定，请刷新核对");}
  }else{
   int n;try{n=db.update("UPDATE fire_video_talk_endpoint SET host=?,sdk_port=?,credential_ref=?,enabled=?,revision=revision+1,update_by=?,update_time=NOW() WHERE camera_id=? AND revision=?",e.host(),e.port(),e.credentialRef(),e.enabled(),user,id,e.revision());}catch(DataAccessException ex){throw new ServiceException("该摄像机地址已被其他监控绑定");}
   if(n!=1)throw new ServiceException("配置已被其他人修改，请关闭后重新加载");
  }return get(id);
 }
 public void delete(long id,int revision){if(db.update("DELETE FROM fire_video_talk_endpoint WHERE camera_id=? AND revision=?",id,revision)!=1)throw new ServiceException("配置已变化，请重新加载");}
 public static boolean privateIpv4(String host){
  if(host==null||!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))return false;
  String[] parts=host.split("\\.");int[] a=new int[4];for(int i=0;i<4;i++){if(parts[i].length()>1&&parts[i].startsWith("0"))return false;a[i]=Integer.parseInt(parts[i]);if(a[i]>255)return false;}
  return a[0]==10||a[0]==172&&a[1]>=16&&a[1]<=31||a[0]==192&&a[1]==168;
 }
}
