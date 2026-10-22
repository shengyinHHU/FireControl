package com.ruoyi.system.fire;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.DisposableBean;
import com.ruoyi.common.exception.ServiceException;
/** Best-effort change signals only. Database remains the source of truth. */
@Component
public class FireAlarmUpdates implements DisposableBean {
 public interface Sink {void send(String event) throws Exception;void close();}
 private record Subscription(long user,Set<Long> gateways,Sink sink,long expires) {}
 private final Map<String,Subscription> subscriptions=new ConcurrentHashMap<>();
 private final Set<Long> pending=new HashSet<>();
 private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"fire-alarm-push");t.setDaemon(true);return t;});
 private long heartbeat;
 private final java.util.function.LongSupplier clock;
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(FireAlarmUpdates.class);
 private final java.util.concurrent.atomic.AtomicBoolean destroyed=new java.util.concurrent.atomic.AtomicBoolean();
 public FireAlarmUpdates(){this(System::currentTimeMillis);}
 public FireAlarmUpdates(java.util.function.LongSupplier time){clock=time;executor.scheduleWithFixedDelay(()->{try{flush();}catch(RuntimeException e){log.warn("实时通知调度异常，下次继续执行",e);}},250,250,TimeUnit.MILLISECONDS);}
 private static void closeQuietly(Sink sink){try{sink.close();}catch(RuntimeException e){log.debug("实时通知连接关闭时已不可用：{}",e.toString());}}
 public synchronized Runnable subscribe(long user,Set<Long> gateways,Sink sink){
  if(destroyed.get())throw new ServiceException("实时通知服务正在关闭");
  if(subscriptions.size()>=200||subscriptions.values().stream().filter(s->s.user()==user).count()>=5)throw new ServiceException("实时通知连接过多，已继续使用轮询");
  String id=UUID.randomUUID().toString();
  var sub=new Subscription(user,Set.copyOf(gateways),sink,clock.getAsLong()+60000);subscriptions.put(id,sub);
  try{sink.send("ready");}catch(Exception e){subscriptions.remove(id);/* Failed sends are completed by the servlet container; do not write/complete again. */}
  return ()->subscriptions.remove(id);
 }
 public void changed(Long gateway){if(!destroyed.get() && gateway!=null)synchronized(pending){pending.add(gateway);}}
 private void flush(){
  Set<Long> changed; synchronized(pending){changed=new HashSet<>(pending);pending.clear();}
  long now=clock.getAsLong();boolean ping=now-heartbeat>=15000;if(ping)heartbeat=now;
  subscriptions.forEach((id,s)->{
   if(now>=s.expires()){if(subscriptions.remove(id,s))closeQuietly(s.sink());return;}
   try{
    if(!Collections.disjoint(changed,s.gateways()))s.sink().send("alarm-change");
    else if(ping)s.sink().send("heartbeat");
   }catch(Exception e){subscriptions.remove(id,s);/* No second completion after a failed write. */}
  });
 }
 @Override public synchronized void destroy(){if(!destroyed.compareAndSet(false,true))return;executor.shutdownNow();var remaining=new ArrayList<>(subscriptions.values());subscriptions.clear();remaining.forEach(s->closeQuietly(s.sink()));synchronized(pending){pending.clear();}}
}
