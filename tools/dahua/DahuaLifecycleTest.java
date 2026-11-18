package com.ruoyi.framework.websocket;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.web.socket.*;
import com.ruoyi.system.fire.dahua.*;

/** Offline lifecycle tests. No camera, native library, real DB or microphone. */
public class DahuaLifecycleTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static void rejects(Runnable r){try{r.run();}catch(RuntimeException expected){return;}throw new AssertionError("Expected rejection");}
 static void until(BooleanSupplier condition)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(!condition.getAsBoolean()){if(System.nanoTime()>end)throw new AssertionError("Timeout");Thread.sleep(10);}}
 static class Store extends DahuaEndpointStore {
  final Endpoint endpoint=new Endpoint(1,"10.5.90.150",37777,"default",true,0);
  Store(DahuaProperties p){super(new AbstractDataSource(){public Connection getConnection(){throw new UnsupportedOperationException();}public Connection getConnection(String u,String p){throw new UnsupportedOperationException();}},null,p);}
  @Override public Endpoint get(long id){return endpoint;}
 }
 static class Sdk extends DahuaSdk {
  volatile CountDownLatch openGate,sendGate;final AtomicInteger started=new AtomicInteger(),stopped=new AtomicInteger(),sent=new AtomicInteger();volatile Consumer<byte[]> callback;
  Sdk(DahuaProperties p){super(p);}
  void await(CountDownLatch gate){if(gate!=null)try{if(!gate.await(3,TimeUnit.SECONDS))throw new AssertionError("Gate timeout");}catch(InterruptedException e){throw new RuntimeException(e);}}
  @Override public Channel open(DahuaEndpointStore.Endpoint e,Consumer<byte[]> audio,Consumer<String> error){started.incrementAndGet();callback=audio;await(openGate);return new Channel(){public long send(byte[] p){await(sendGate);sent.incrementAndGet();return p.length+44;}public void close(){stopped.incrementAndGet();}};}
 }
 static class Socket {
  final Map<String,Object> attributes=new ConcurrentHashMap<>();final List<WebSocketMessage<?>> messages=new CopyOnWriteArrayList<>();final AtomicBoolean open=new AtomicBoolean(true);
  WebSocketSession value=(WebSocketSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebSocketSession.class},(p,m,a)->switch(m.getName()){
   case "getAttributes"->attributes;case "isOpen"->open.get();case "getId"->"fixture";case "sendMessage"->{messages.add((WebSocketMessage<?>)a[0]);yield null;}case "close"->{open.set(false);yield null;}default->null;
  });
 }
 static String ticket(DahuaTalkService service,long camera,long owner){return (String)service.ticket(camera,owner).get("ticket");}
 public static void main(String[] args)throws Exception{
  var p=new DahuaProperties();var credential=new DahuaProperties.Credential();credential.setPassword("offline-fixture-only");p.getCredentials().put("default",credential);var sdk=new Sdk(p);var service=new DahuaTalkService(sdk,new Store(p),p);
  try{
   check(DahuaEndpointStore.privateIpv4("10.5.90.150"));check(DahuaEndpointStore.privateIpv4("172.16.0.1"));check(!DahuaEndpointStore.privateIpv4("8.8.8.8"));check(!DahuaEndpointStore.privateIpv4("127.0.0.1"));check(!DahuaEndpointStore.privateIpv4("10.5.090.150"));check(!DahuaEndpointStore.privateIpv4("10.5.90.999"));
   String key=ticket(service,1,7);rejects(()->ticket(service,1,8));rejects(()->ticket(service,2,7));
   service.cancel(key,8);var socket=new Socket();var call=service.authenticate(key,socket.value);rejects(()->service.authenticate(key,new Socket().value));
   until(()->socket.messages.stream().anyMatch(m->m instanceof TextMessage&&m.getPayload().toString().contains("ready")));
   call.audio(new byte[640]);until(()->sdk.sent.get()==1);sdk.callback.accept(new byte[332]);until(()->socket.messages.stream().anyMatch(m->m instanceof BinaryMessage));
   call.audio(new byte[639]);until(()->sdk.stopped.get()==1);check(!socket.open.get());rejects(()->service.authenticate(key,new Socket().value));
   // A cancelled login finishing late must still close the native call and release its lease.
   sdk.openGate=new CountDownLatch(1);String late=ticket(service,1,7);service.authenticate(late,new Socket().value);until(()->sdk.started.get()==2);service.cancel(late,7);sdk.openGate.countDown();until(()->sdk.stopped.get()==2);sdk.openGate=null;
   // Drop queued speech on overflow, then release the native session after the active send.
   String slow=ticket(service,1,7);var slowSocket=new Socket();var slowCall=service.authenticate(slow,slowSocket.value);until(()->slowSocket.messages.size()>0);sdk.sendGate=new CountDownLatch(1);slowCall.audio(new byte[640]);Thread.sleep(30);for(int i=0;i<12;i++)slowCall.audio(new byte[640]);sdk.sendGate.countDown();until(()->sdk.stopped.get()==3);check(sdk.sent.get()<=2);sdk.sendGate=null;
   String expires=ticket(service,1,7);var timed=service.authenticate(expires,new Socket().value);timed.last=0;until(()->sdk.stopped.get()==4);
   p.getCredentials().clear();rejects(()->ticket(service,1,7));
   System.out.println("PASS: private IP validation; ownership; exclusive leases; one-use auth; PCM bounds; native submission; downlink; late cancel; bounded queue; heartbeat expiry");
  }finally{service.destroy();sdk.close();}
 }
}
