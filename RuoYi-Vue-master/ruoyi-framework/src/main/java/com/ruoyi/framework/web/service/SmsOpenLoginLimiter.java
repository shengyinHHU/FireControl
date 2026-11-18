package com.ruoyi.framework.web.service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import com.ruoyi.system.fire.sms.*;
@Component
public class SmsOpenLoginLimiter {
    private final StringRedisTemplate redis;
    private final FireSmsOpenProperties config;
    private static final DefaultRedisScript<Long> COUNT=new DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],65) end; return n",Long.class);
    public SmsOpenLoginLimiter(StringRedisTemplate redis,FireSmsOpenProperties config){this.redis=redis;this.config=config;}
    public void check(String remoteAddress){
        try {
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(remoteAddress.getBytes(StandardCharsets.UTF_8)));
            Long count=redis.execute(COUNT,List.of("sms_open:login:"+hash+":"+(System.currentTimeMillis()/60000)));
            if(count==null)throw new IllegalStateException("Missing counter");
            if(count>config.getLoginLimit())throw new FireSmsOpenException(429,"获取Token过于频繁，请稍后再试",60);
        } catch(FireSmsOpenException e){throw e;}
        catch(Exception unavailable){throw new FireSmsOpenException(503,"认证服务暂不可用，请稍后再试");}
    }
}
