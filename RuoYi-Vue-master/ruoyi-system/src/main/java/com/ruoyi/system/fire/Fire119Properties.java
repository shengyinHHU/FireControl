package com.ruoyi.system.fire;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="fire.incident119")
public class Fire119Properties {
 private boolean enabled;
 private String baseUrl="https://wdsjs.jiangning.gov.cn:9091/zhjn-order/api/api/open";
 private String orderSource,account,password,publicKey,sourceSystem="jiangning_119",regionCode="320116";
 private String sourceCoordinateSystem="unknown";
 private int intervalSeconds=60,initialDelaySeconds=10,pageSize=20,maxPages=500,requestTimeoutSeconds=15,maxRunSeconds=240,tokenLifetimeSeconds=600;
 private Long deptId,supervisorUserId;
 private String boundaryCoordinateMode="pending";
 public String getBoundaryCoordinateMode(){return boundaryCoordinateMode;} public void setBoundaryCoordinateMode(String v){boundaryCoordinateMode=v;}
 public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
 public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String v){baseUrl=v;}
 public String getOrderSource(){return orderSource;} public void setOrderSource(String v){orderSource=v;}
 public String getAccount(){return account;} public void setAccount(String v){account=v;}
 public String getPassword(){return password;} public void setPassword(String v){password=v;}
 public String getPublicKey(){return publicKey;} public void setPublicKey(String v){publicKey=v;}
 public String getSourceSystem(){return sourceSystem;} public void setSourceSystem(String v){sourceSystem=v;}
 public String getRegionCode(){return regionCode;} public void setRegionCode(String v){regionCode=v;}
 public String getSourceCoordinateSystem(){return sourceCoordinateSystem;} public void setSourceCoordinateSystem(String v){sourceCoordinateSystem=v;}
 public int getIntervalSeconds(){return intervalSeconds;} public void setIntervalSeconds(int v){intervalSeconds=v;}
 public int getInitialDelaySeconds(){return initialDelaySeconds;} public void setInitialDelaySeconds(int v){initialDelaySeconds=v;}
 public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}
 public int getMaxPages(){return maxPages;} public void setMaxPages(int v){maxPages=v;}
 public int getRequestTimeoutSeconds(){return requestTimeoutSeconds;} public void setRequestTimeoutSeconds(int v){requestTimeoutSeconds=v;}
 public int getMaxRunSeconds(){return maxRunSeconds;} public void setMaxRunSeconds(int v){maxRunSeconds=v;}
 public int getTokenLifetimeSeconds(){return tokenLifetimeSeconds;} public void setTokenLifetimeSeconds(int v){tokenLifetimeSeconds=v;}
 public Long getDeptId(){return deptId;} public void setDeptId(Long v){deptId=v;}
 public Long getSupervisorUserId(){return supervisorUserId;} public void setSupervisorUserId(Long v){supervisorUserId=v;}
}
