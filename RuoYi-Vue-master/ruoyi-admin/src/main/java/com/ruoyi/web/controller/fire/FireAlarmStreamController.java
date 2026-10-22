package com.ruoyi.web.controller.fire;
import java.util.HashSet;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.FireAccessService;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.fire.FireAlarmUpdates;
@RestController
@RequestMapping("/fire/alarms")
public class FireAlarmStreamController {
 private final FireAlarmUpdates updates;private final FireAccessService service;
 public FireAlarmStreamController(FireAlarmUpdates u,FireAccessService s){updates=u;service=s;}
 @PreAuthorize("@ss.hasPermi('fire:alarms:list')")
 @GetMapping(value="/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
 public SseEmitter stream(HttpServletResponse response){
  response.setHeader("Cache-Control","no-store");response.setHeader("X-Accel-Buffering","no");
  var emitter=new SseEmitter(60000L);
  var sink=new FireAlarmEmitter(emitter);
  Runnable unsubscribe=updates.subscribe(SecurityUtils.getUserId(),new HashSet<>(service.alarmGatewayIds(new FireAccessQuery())),sink);
  emitter.onCompletion(()->{sink.unusable();unsubscribe.run();});
  emitter.onTimeout(()->{unsubscribe.run();sink.close();});
  emitter.onError(e->{sink.unusable();unsubscribe.run();});
  return emitter;
 }
}
