package com.ruoyi.system.fire.dahua;

import java.util.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Component
@ConfigurationProperties(prefix="fire.video.dahua")
public class DahuaProperties {
 private boolean enabled=true;
 private String extractDirectory="./runtime/dahua-sdk";
 private int maxSessions=4;
 private List<String> allowedOrigins=new ArrayList<>(List.of("http://localhost","http://localhost:5173","http://127.0.0.1:5173"));
 private Map<String,Credential> credentials=new LinkedHashMap<>();
 private Map<String,Tunnel> tunnels=new LinkedHashMap<>();
 /** Server configuration only. Browser bindings continue to use real private device addresses. */
 public static class Tunnel {
  private String deviceHost=""; private int devicePort=37777,localPort=33777;
  public String getDeviceHost(){return deviceHost;}public void setDeviceHost(String v){deviceHost=v;}
  public int getDevicePort(){return devicePort;}public void setDevicePort(int v){devicePort=v;}
  public int getLocalPort(){return localPort;}public void setLocalPort(int v){localPort=v;}
 }
 public Map<String,Tunnel> getTunnels(){return tunnels;}public void setTunnels(Map<String,Tunnel> v){tunnels=v;}
 @PostConstruct public void validateTunnels(){
  if(tunnels==null||tunnels.size()>8)throw new IllegalStateException("对讲通道配置无效，最多8台摄像机");
  Set<String> devices=new HashSet<>();Set<Integer> ports=new HashSet<>();
  for(var entry:tunnels.entrySet()){
   Tunnel t=entry.getValue();
   if(entry.getKey()==null||!entry.getKey().matches("[A-Za-z0-9_.-]{1,32}")||t==null||!DahuaEndpointStore.privateIpv4(t.deviceHost)||t.devicePort<1||t.devicePort>65535||t.localPort<1024||t.localPort>65535||!devices.add(t.deviceHost+":"+t.devicePort)||!ports.add(t.localPort))throw new IllegalStateException("对讲通道需唯一的专网摄像机地址和服务器本机端口");
  }
 }
 public DahuaEndpointStore.Endpoint resolveEndpoint(DahuaEndpointStore.Endpoint endpoint){
  validateTunnels();
  for(Tunnel t:tunnels.values())if(t.deviceHost.equals(endpoint.host())&&t.devicePort==endpoint.port())
   return new DahuaEndpointStore.Endpoint(endpoint.cameraId(),"127.0.0.1",t.localPort,endpoint.credentialRef(),endpoint.enabled(),endpoint.revision());
  return endpoint;
 }
 public static class Credential {
  private String username="admin",password="";
  public String getUsername(){return username;} public void setUsername(String v){username=v;}
  public String getPassword(){return password;} public void setPassword(String v){password=v;}
 }
 public boolean isEnabled(){return enabled;}public void setEnabled(boolean v){enabled=v;}
 public String getExtractDirectory(){return extractDirectory;}public void setExtractDirectory(String v){extractDirectory=v;}
 public int getMaxSessions(){return Math.max(1,Math.min(16,maxSessions));}public void setMaxSessions(int v){maxSessions=v;}
 public List<String> getAllowedOrigins(){return allowedOrigins;}public void setAllowedOrigins(List<String> v){allowedOrigins=v;}
 public Map<String,Credential> getCredentials(){return credentials;}public void setCredentials(Map<String,Credential> v){credentials=v;}
 public Credential credential(String ref){var c=credentials.get(ref);if(c==null||c.getPassword()==null||c.getPassword().isBlank())throw new IllegalStateException("摄像机登录配置未完成，请管理员设置服务器凭据");return c;}
}
