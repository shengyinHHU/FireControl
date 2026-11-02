package com.ruoyi.system.fire;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;
@Component
public class FireConnections {
 private final ConcurrentMap<String,ConcurrentMap<String,Long>> peers=new ConcurrentHashMap<>();
 private final ConcurrentMap<String,Long> generations=new ConcurrentHashMap<>();
 private final ConcurrentMap<String,Runnable> closers=new ConcurrentHashMap<>();
 private final FireDeviceProperties properties;
 public FireConnections(FireDeviceProperties p){properties=p;}
 public void register(String session,Runnable close){closers.put(session,close);}
 public void unregister(String session){closers.remove(session);}
 public long generation(String source){return source==null?0:generations.getOrDefault(source,0L);}
 public synchronized boolean seenCurrent(String source,String session,long generation){if(generation(source)!=generation)return false;seen(source,session);return true;}
 public synchronized void invalidate(String source){if(source==null)return;generations.merge(source,1L,Long::sum);var sessions=peers.remove(source);if(sessions!=null)for(String id:sessions.keySet()){var close=closers.get(id);if(close!=null)close.run();}}
 public void seen(String source,String session){peers.computeIfAbsent(source,k->new ConcurrentHashMap<>()).put(session,System.currentTimeMillis());}
 public void closed(String source,String session){if(source!=null){var sessions=peers.get(source);if(sessions!=null)sessions.remove(session);}}
 public boolean online(String source){if(source==null)return false;var sessions=peers.get(source);return sessions!=null && sessions.values().stream().anyMatch(t->System.currentTimeMillis()-t<=properties.getOnlineSeconds()*1000L);}
}
