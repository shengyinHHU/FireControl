package com.ruoyi.system.fire;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;
@Component
public class FireConnections {
 private final ConcurrentMap<String,ConcurrentMap<String,Long>> peers=new ConcurrentHashMap<>();
 private final FireDeviceProperties properties;
 public FireConnections(FireDeviceProperties p){properties=p;}
 public void seen(String source,String session){peers.computeIfAbsent(source,k->new ConcurrentHashMap<>()).put(session,System.currentTimeMillis());}
 public void closed(String source,String session){if(source!=null){var sessions=peers.get(source);if(sessions!=null)sessions.remove(session);}}
 public boolean online(String source){var sessions=peers.get(source);return sessions!=null && sessions.values().stream().anyMatch(t->System.currentTimeMillis()-t<=properties.getOnlineSeconds()*1000L);}
}
