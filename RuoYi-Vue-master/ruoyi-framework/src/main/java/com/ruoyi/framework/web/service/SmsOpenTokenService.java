package com.ruoyi.framework.web.service;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import com.ruoyi.system.fire.sms.FireSmsOpenException;
@Component
public class SmsOpenTokenService {
    private final SmsOpenAccess access;private final SmsOpenLoginLimiter limiter;private final SysLoginService login;private final TokenService tokens;
    public SmsOpenTokenService(SmsOpenAccess access,SmsOpenLoginLimiter limiter,SysLoginService login,TokenService tokens){this.access=access;this.limiter=limiter;this.login=login;this.tokens=tokens;}
    public Map<String,Object> token(Map<String,Object> input,HttpServletRequest request){
        access.deployment(request);limiter.check(request.getRemoteAddr());
        if(!input.keySet().equals(Set.of("username","password")))throw new FireSmsOpenException(400,"请仅提供username和password");
        if(!(input.get("username")instanceof String username)||username.length()<2||username.length()>20||
                !(input.get("password")instanceof String password)||password.length()<5||password.length()>20)
            throw new FireSmsOpenException(401,"账号或密码不正确，或账号未开通短信接口");
        try {
            access.precheck(username);
            String token=login.loginSmsServiceAccount(username,password,access::mark);
            return Map.of("token",token,"tokenType","Bearer","expiresIn",tokens.getExpireMinutes()*60);
        }catch(org.springframework.dao.DataAccessException unavailable){throw new FireSmsOpenException(503,"认证服务暂不可用，请稍后再试");}
        catch(Exception denied){throw new FireSmsOpenException(401,"账号或密码不正确、已锁定或未开通短信接口");}
    }
}
