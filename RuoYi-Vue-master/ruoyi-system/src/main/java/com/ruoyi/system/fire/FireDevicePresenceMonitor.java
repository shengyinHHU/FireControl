package com.ruoyi.system.fire;

import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import com.ruoyi.system.mapper.FireAccessMapper;

/** Silence cannot report its own timeout; use the same presence rule as list reads. */
@Component
@EnableScheduling
public class FireDevicePresenceMonitor {
 private final FireAccessMapper mapper;private final FireConnections connections;private final FireAlarmUpdates updates;
 private Map<Long,Boolean> previous=new HashMap<>();
 public FireDevicePresenceMonitor(FireAccessMapper m,FireConnections c,FireAlarmUpdates u){mapper=m;connections=c;updates=u;}
 @Scheduled(fixedDelay=5000)
 public synchronized void check(){
  Map<Long,Boolean> current=new HashMap<>();
  for(var row:mapper.gatewayPresence()){
   Long id=((Number)row.get("id")).longValue();boolean online=connections.online((String)row.get("ip"),row.get("receivedAt"));current.put(id,online);
   if(previous.containsKey(id)&&previous.get(id)!=online)updates.deviceChanged(id);
  }
  previous=current;
 }
}
