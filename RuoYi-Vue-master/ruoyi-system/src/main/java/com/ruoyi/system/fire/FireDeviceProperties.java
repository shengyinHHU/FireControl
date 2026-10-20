package com.ruoyi.system.fire;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix="fire.device")
public class FireDeviceProperties {
 private boolean enabled=false; private String bindAddress="10.5.90.50";
 private int port=2000,maxConnections=32,idleSeconds=600,onlineSeconds=600;
 public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
 public String getBindAddress(){return bindAddress;}public void setBindAddress(String v){bindAddress=v;}
 public int getPort(){return port;}public void setPort(int v){port=v;}
 public int getMaxConnections(){return maxConnections;}public void setMaxConnections(int v){maxConnections=v;}
 public int getIdleSeconds(){return idleSeconds;}public void setIdleSeconds(int v){idleSeconds=v;}
 public int getOnlineSeconds(){return onlineSeconds;}public void setOnlineSeconds(int v){onlineSeconds=v;}
}
