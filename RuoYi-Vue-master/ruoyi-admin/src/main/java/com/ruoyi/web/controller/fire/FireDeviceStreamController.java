package com.ruoyi.web.controller.fire;

import java.util.HashSet;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.fire.FireAlarmUpdates;
import com.ruoyi.system.service.FireAccessService;

@RestController
@RequestMapping("/fire/devices")
public class FireDeviceStreamController {
 private final FireAlarmUpdates updates;private final FireAccessService service;
 public FireDeviceStreamController(FireAlarmUpdates u,FireAccessService s){updates=u;service=s;}
 @PreAuthorize("@ss.hasAnyPermi('fire:gateways:list','fire:alarms:list')")
 @GetMapping(value="/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
 public SseEmitter stream(@RequestParam(defaultValue="gateways") String resource,HttpServletResponse response){
  if(!resource.equals("gateways")&&!resource.equals("alarms"))throw new IllegalArgumentException("不支持的实时通知范围");
  if(!SecurityUtils.hasPermi(resource.equals("alarms")?"fire:alarms:list":"fire:gateways:list"))throw new AccessDeniedException("无权订阅此类设备记录");
  var captured=SecurityContextHolder.createEmptyContext();captured.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
  response.setHeader("Cache-Control","no-store");response.setHeader("X-Accel-Buffering","no");
  var emitter=new SseEmitter(60000L);var sink=new FireAlarmEmitter(emitter);
  Runnable unsubscribe=updates.subscribeDevices(SecurityUtils.getUserId(),()->{
   var previous=SecurityContextHolder.getContext();
   try{SecurityContextHolder.setContext(captured);return new HashSet<>(resource.equals("alarms")?service.alarmGatewayIds(new FireAccessQuery()):service.deviceGatewayIds(new FireAccessQuery()));}
   finally{SecurityContextHolder.setContext(previous);}
  },sink);
  emitter.onCompletion(()->{sink.unusable();unsubscribe.run();});
  emitter.onTimeout(()->{unsubscribe.run();sink.close();});
  emitter.onError(e->{sink.unusable();unsubscribe.run();});
  return emitter;
 }
}
