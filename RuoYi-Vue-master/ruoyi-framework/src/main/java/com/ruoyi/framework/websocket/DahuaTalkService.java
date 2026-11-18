package com.ruoyi.framework.websocket;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import tools.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.fire.dahua.*;

/** Short-lived, single-use WS credentials. Native operations run off servlet/SDK threads. */
@Service
public class DahuaTalkService {
 private static final Logger log=LoggerFactory.getLogger(DahuaTalkService.class);
 private final DahuaSdk sdk;private final DahuaEndpointStore store;private final DahuaProperties config;
 private final ObjectMapper json=new ObjectMapper();
 private final Map<String,Session> sessions=new HashMap<>();
 private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"dahua-watchdog");t.setDaemon(true);return t;});
 public DahuaTalkService(DahuaSdk s,DahuaEndpointStore e,DahuaProperties c){sdk=s;store=e;config=c;timer.scheduleAtFixedRate(this::expire,1,1,TimeUnit.SECONDS);}
 public synchronized Map<String,Object> ticket(long camera,long owner){
  if(!config.isEnabled())throw new ServiceException("大华对讲未启用");
  if(sessions.size()>=config.getMaxSessions())throw new ServiceException("对讲通道已满，请稍后再试");
  if(sessions.values().stream().anyMatch(s->s.camera==camera||s.owner==owner))throw new ServiceException("设备或当前用户已有对讲，请先结束");
  var endpoint=store.get(camera);if(endpoint==null||!endpoint.enabled())throw new ServiceException("此监控尚未启用大华连接，请管理员配置摄像机IP");
  config.credential(endpoint.credentialRef());
  String key=UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","");
  Session call=new Session(key,camera,owner,endpoint);sessions.put(key,call);
  return Map.of("ticket",key,"path","/fire/video/dahua/socket","expiresIn",30);
 }
 public synchronized void cancel(String key,long owner){Session s=sessions.get(key);if(s!=null&&s.owner==owner)s.finish(null);}
 public synchronized Session authenticate(String key,WebSocketSession socket){
  Session s=sessions.get(key);if(s==null||s.socket!=null||System.currentTimeMillis()-s.created>30000||s.closed.get())throw new ServiceException("对讲凭证无效或过期");
  s.socket=socket;s.last=System.currentTimeMillis();s.enqueue(()->s.open());return s;
 }
 public synchronized DahuaEndpointStore.Endpoint save(long camera,DahuaEndpointStore.Edit e,String user){idle(camera);return store.save(camera,e,user);}
 public synchronized void delete(long camera,int revision){idle(camera);store.delete(camera,revision);}
 private void idle(long id){if(sessions.values().stream().anyMatch(s->s.camera==id))throw new ServiceException("设备正在对讲，请结束后再修改连接");}
 private void expire(){List<Session> all;synchronized(this){all=List.copyOf(sessions.values());}long now=System.currentTimeMillis();for(Session s:all)if(now-s.created>600000||now-s.last>(s.socket==null?30000:15000))s.finish("对讲超时，连接已释放");}
 public final class Session {
  final String key;final long camera,owner,created=System.currentTimeMillis();final DahuaEndpointStore.Endpoint endpoint;
  volatile long last=created;volatile WebSocketSession socket;volatile DahuaSdk.Channel nativeCall;final AtomicBoolean closed=new AtomicBoolean();
  final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(6),r->{Thread t=new Thread(r,"dahua-talk");t.setDaemon(true);return t;});
  long sent,received;
  Session(String k,long c,long o,DahuaEndpointStore.Endpoint e){key=k;camera=c;owner=o;endpoint=e;}
  void enqueue(Runnable task){if(closed.get())return;try{worker.execute(()->{if(!closed.get())try{task.run();}catch(Exception|LinkageError e){log.warn("Dahua talk camera={} failed: {}",camera,e.getMessage());finish(e.getMessage());}});}catch(RejectedExecutionException e){finish("音频积压，对讲已停止，请检查网络");}}
  void open(){
   nativeCall=sdk.open(endpoint,data->enqueue(()->{send(new BinaryMessage(data));received+=data.length-12;}),this::finish);
   if(closed.get())return; // queued finish owns cleanup, including late logins
   send(new TextMessage("{\"type\":\"ready\",\"sampleRate\":8000}"));log.info("Dahua talk started camera={} user={}",camera,owner);
  }
  public void heartbeat(){last=System.currentTimeMillis();enqueue(()->send(new TextMessage("{\"type\":\"pong\",\"submittedBytes\":"+sent+",\"receivedBytes\":"+received+"}")));}
  public void audio(byte[] data){if(data.length!=640){finish("无效音频包");return;}last=System.currentTimeMillis();enqueue(()->{if(nativeCall==null)throw new IllegalStateException("对讲尚未就绪");nativeCall.send(data);sent+=data.length;});}
  void send(WebSocketMessage<?> message){try{if(socket!=null&&socket.isOpen())socket.sendMessage(message);}catch(Exception e){finish("浏览器连接已断开");}}
  public void finish(String reason){
   if(!closed.compareAndSet(false,true))return;
   worker.getQueue().clear();
   worker.execute(()->{
    try{if(reason!=null)send(new TextMessage(json.writeValueAsString(Map.of("type","error","message",reason))));if(nativeCall!=null)nativeCall.close();}
    finally{try{if(socket!=null)socket.close(CloseStatus.NORMAL);}catch(Exception ignored){}synchronized(DahuaTalkService.this){sessions.remove(key);}log.info("Dahua talk stopped camera={} user={} sentBytes={} receivedBytes={} reason={}",camera,owner,sent,received,reason==null?"user/disconnect":reason);worker.shutdown();}
   });
  }
 }
 @PreDestroy public void destroy(){timer.shutdownNow();List<Session> all;synchronized(this){all=List.copyOf(sessions.values());}all.forEach(s->s.finish(null));for(Session s:all)try{s.worker.awaitTermination(8,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
