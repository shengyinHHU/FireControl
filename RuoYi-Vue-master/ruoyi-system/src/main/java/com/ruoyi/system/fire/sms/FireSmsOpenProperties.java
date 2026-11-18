package com.ruoyi.system.fire.sms;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix="fire.sms.open")
public class FireSmsOpenProperties {
    private boolean enabled;
    private int maxRecipients=10, minuteLimit=30, dailyLimit=500, loginLimit=30;
    private List<String> allowedIps=new ArrayList<>();
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    public int getMaxRecipients(){return maxRecipients;} public void setMaxRecipients(int v){maxRecipients=v;}
    public int getMinuteLimit(){return minuteLimit;} public void setMinuteLimit(int v){minuteLimit=v;}
    public int getDailyLimit(){return dailyLimit;} public void setDailyLimit(int v){dailyLimit=v;}
    public int getLoginLimit(){return loginLimit;} public void setLoginLimit(int v){loginLimit=v;}
    public List<String> getAllowedIps(){return allowedIps;} public void setAllowedIps(List<String> v){allowedIps=v;}
    public void ready() {
        if(!enabled)throw new FireSmsOpenException(503,"短信开放服务未启用");
        if(maxRecipients<1||maxRecipients>10||minuteLimit<1||dailyLimit<1||loginLimit<1)
            throw new FireSmsOpenException(503,"短信开放服务限额配置无效");
    }
}
