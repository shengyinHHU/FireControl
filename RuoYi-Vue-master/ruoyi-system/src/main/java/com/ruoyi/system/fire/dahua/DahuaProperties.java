package com.ruoyi.system.fire.dahua;

import java.util.*;
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
