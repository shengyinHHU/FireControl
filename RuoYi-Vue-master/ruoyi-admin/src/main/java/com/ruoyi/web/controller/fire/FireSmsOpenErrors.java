package com.ruoyi.web.controller.fire;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.fire.sms.FireSmsOpenException;
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes=FireSmsOpenController.class)
public class FireSmsOpenErrors {
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String,Object>> service(ServiceException e){
        int status=e.getCode()==null?503:e.getCode();if(status<400||status>599)status=500;
        return ResponseEntity.status(status).header("Cache-Control","no-store").body(Map.of("code",status,"msg",e.getMessage()));
    }
    @ExceptionHandler(FireSmsOpenException.class)
    public ResponseEntity<Map<String,Object>> api(FireSmsOpenException e){
        var response=ResponseEntity.status(e.getCode()).header("Cache-Control","no-store");
        if(e.getRetryAfter()>0)response.header("Retry-After",Long.toString(e.getRetryAfter()));
        return response.body(Map.of("code",e.getCode(),"msg",e.getMessage()));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String,Object>> invalid(Exception e){return ResponseEntity.badRequest().header("Cache-Control","no-store").body(Map.of("code",400,"msg","请求必须是有效的JSON对象，参数类型需符合接口说明"));}
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String,Object>> contentType(Exception e){return ResponseEntity.status(415).header("Cache-Control","no-store").body(Map.of("code",415,"msg","Content-Type的值必须是application/json；Postman请选择Body → raw → JSON"));}
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String,Object>> denied(Exception e){return ResponseEntity.status(403).body(Map.of("code",403,"msg","没有此接口权限"));}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> unavailable(Exception e){return ResponseEntity.status(500).header("Cache-Control","no-store").body(Map.of("code",500,"msg","暂未取得可靠结果。发送请求请保留原requestId并先查询，不要直接更换编号重发"));}
}
