package com.ruoyi.system.fire.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fire.sms")
public class FireSmsProperties {
    private boolean enabled;
    private String endpoint = "https://qxtapi.js118114.com:9003/SMSController/sendSms.do";
    private String spid = "1000048804", authCode = "", authKey = "";
    private String signature = "【大厂街道办事处】";
    // Application limit, not a claim about vendor billing or segment length.
    private int maxContentLength = 1000;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String v) { endpoint = v; }
    public String getSpid() { return spid; }
    public void setSpid(String v) { spid = v; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String v) { authCode = v; }
    public String getAuthKey() { return authKey; }
    public void setAuthKey(String v) { authKey = v; }
    public String getSignature() { return signature; }
    public void setSignature(String v) { signature = v; }
    public int getMaxContentLength() { return maxContentLength; }
    public void setMaxContentLength(int v) { maxContentLength = v; }
}
