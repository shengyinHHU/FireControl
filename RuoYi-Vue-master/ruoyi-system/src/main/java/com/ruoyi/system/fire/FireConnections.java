package com.ruoyi.system.fire;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;
/** Connection keys are observed source IPs, never protocol addresses. */
@Component
public class FireConnections {
 private final ConcurrentMap<String,ConcurrentMap<String,Long>> peers=new ConcurrentHashMap<>();
 private final ConcurrentMap<String,Long> generations=new ConcurrentHashMap<>();
 private final ConcurrentMap<String,Runnable> closers=new ConcurrentHashMap<>();
 private final FireDeviceProperties properties;
 public FireConnections(FireDeviceProperties p){properties=p;}
 public boolean recent(java.util.Date receivedAt){long age=receivedAt==null?Long.MAX_VALUE:System.currentTimeMillis()-receivedAt.getTime();return age>=0 && age<=properties.getOnlineSeconds()*1000L;}
 public void register(String session,Runnable close){closers.put(session,close);}
 public void unregister(String session){closers.remove(session);}
 public long generation(String ip){return ip==null?0:generations.getOrDefault(ip,0L);}
 public synchronized boolean seenCurrent(String ip,String session,long generation){if(generation(ip)!=generation)return false;seen(ip,session);return true;}
 public synchronized void invalidate(String ip){if(ip==null)return;generations.merge(ip,1L,Long::sum);var sessions=peers.remove(ip);if(sessions!=null)for(String id:sessions.keySet()){var close=closers.get(id);if(close!=null)close.run();}}
 public void seen(String ip,String session){peers.computeIfAbsent(ip,k->new ConcurrentHashMap<>()).put(session,System.currentTimeMillis());}
 public void closed(String ip,String session){if(ip!=null){var sessions=peers.get(ip);if(sessions!=null)sessions.remove(session);}}
 public boolean online(String ip){if(ip==null)return false;var sessions=peers.get(ip);return sessions!=null && sessions.values().stream().anyMatch(t->System.currentTimeMillis()-t<=properties.getOnlineSeconds()*1000L);}
}
