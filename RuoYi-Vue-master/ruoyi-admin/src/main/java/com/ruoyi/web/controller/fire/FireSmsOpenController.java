package com.ruoyi.web.controller.fire;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.framework.web.service.SmsOpenAccess;
import com.ruoyi.framework.web.service.SmsOpenTokenService;
import com.ruoyi.system.fire.sms.FireSmsOpenService;
@RestController
@RequestMapping("/fire/sms/open")
public class FireSmsOpenController {
    private final SmsOpenTokenService tokens;private final SmsOpenAccess access;private final FireSmsOpenService sms;
    public FireSmsOpenController(SmsOpenTokenService tokens,SmsOpenAccess access,FireSmsOpenService sms){this.tokens=tokens;this.access=access;this.sms=sms;}
    @PostMapping("/token")
    public AjaxResult token(@RequestBody Map<String,Object> input,HttpServletRequest request,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");
        var result=AjaxResult.success();result.putAll(tokens.token(input,request));return result;
    }
    @PostMapping("/send")
    public AjaxResult send(@RequestBody Map<String,Object> input,HttpServletRequest request,HttpServletResponse response)throws Exception{
        response.setHeader("Cache-Control","no-store");
        access.current(request,"fire:sms:open:send");return AjaxResult.success(sms.send(input));
    }
    @GetMapping("/requests/{requestId}")
    public AjaxResult query(@PathVariable String requestId,HttpServletRequest request,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");
        access.current(request,"fire:sms:open:query");return AjaxResult.success(sms.query(requestId));
    }
}
