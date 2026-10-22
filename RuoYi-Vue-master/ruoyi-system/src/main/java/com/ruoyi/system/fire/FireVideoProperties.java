package com.ruoyi.system.fire;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Component
@ConfigurationProperties(prefix="fire.video")
public class FireVideoProperties {
 private boolean enabled=true;
 private String appId,appSecret,enterpriseUser;
 private List<Long> allowedUserIds=new ArrayList<>();
 public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
 public String getAppId(){return appId;} public void setAppId(String v){appId=v;}
 public String getAppSecret(){return appSecret;} public void setAppSecret(String v){appSecret=v;}
 public String getEnterpriseUser(){return enterpriseUser;} public void setEnterpriseUser(String v){enterpriseUser=v;}
 public List<Long> getAllowedUserIds(){return allowedUserIds;} public void setAllowedUserIds(List<Long> v){allowedUserIds=v;}
}
