package com.ruoyi.system.fire;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** Bounded JDK socket workers; no new network runtime dependency required. */
@Component
public class FireTcpServer implements SmartLifecycle {
 private static final Logger log=LoggerFactory.getLogger(FireTcpServer.class);
 private final FireDeviceProperties config;private final FireIngestService ingest;private final FireConnections connections;
 private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
 private volatile boolean running;private ServerSocket listener;private ExecutorService workers;private Thread acceptThread;
 public FireTcpServer(FireDeviceProperties c,FireIngestService i,FireConnections r){config=c;ingest=i;connections=r;}
 @Override public synchronized void start(){
  if(running || !config.isEnabled())return;
  if(config.getPort()<1 || config.getPort()>65535 || config.getMaxConnections()<1 || config.getMaxConnections()>256 || config.getIdleSeconds()<1 || config.getOnlineSeconds()<1)throw new IllegalArgumentException("fire.device 参数无效");
  try{
   listener=new ServerSocket();listener.setReuseAddress(false);
   listener.bind(new InetSocketAddress(config.getBindAddress(),config.getPort()),16);
   workers=new ThreadPoolExecutor(config.getMaxConnections(),config.getMaxConnections(),0,TimeUnit.SECONDS,new SynchronousQueue<>(),r->{Thread t=new Thread(r,"fire-device-session");t.setDaemon(true);return t;});
   running=true;acceptThread=new Thread(this::accept,"fire-device-accept");acceptThread.setDaemon(true);acceptThread.start();
   log.info("消防TCP接入已启动 {}:{}",config.getBindAddress(),config.getPort());
  }catch(IOException e){if(listener!=null)try{listener.close();}catch(IOException ignored){}throw new IllegalStateException("消防TCP监听启动失败",e);}
 }
 private void accept(){
  while(running)try{
   Socket socket=listener.accept();socket.setSoTimeout(1000);sockets.add(socket);
   try{workers.execute(()->session(socket));}catch(RejectedExecutionException e){sockets.remove(socket);socket.close();log.warn("消防TCP连接数达到上限");}
  }catch(IOException e){if(running)log.error("消防TCP接收连接失败",e);}
 }
 private void session(Socket socket){
  String session=UUID.randomUUID().toString();Set<String> sources=new HashSet<>();
  String ip=socket.getInetAddress().getHostAddress();Gb26875.Stream stream=new Gb26875.Stream();long last=System.currentTimeMillis();
  try(socket){
   byte[] buffer=new byte[8192];InputStream input=socket.getInputStream();
   while(running){
    int n;
    try{n=input.read(buffer);}catch(SocketTimeoutException e){if(System.currentTimeMillis()-last>config.getIdleSeconds()*1000L)break;continue;}
    if(n<0)break;last=System.currentTimeMillis();
    byte[] chunk=Arrays.copyOf(buffer,n);
    try{ingest.rawChunk(session,ip,chunk);}catch(Exception e){log.error("TCP原始数据保存失败，不返回确认",e);return;}
    for(Gb26875.Packet packet:stream.feed(chunk)){
     FireIngestService.Receipt receipt;
     try{receipt=ingest.receive(packet,session,ip);}
     catch(Exception e){log.error("消防报文持久化失败，不发送成功确认；session={}",session,e);continue;}
     if(receipt.source()!=null && packet.frame()!=null){sources.add(receipt.source());connections.seen(receipt.source(),session);}
     if(receipt.reply()!=null){
      try{
       socket.getOutputStream().write(receipt.reply());socket.getOutputStream().flush();
      }catch(IOException e){ingest.transmission(receipt.sendId(),"FAILED","TCP发送失败，交付情况未知");throw e;}
      // SENT means submitted to the local TCP stack; peer receipt is not asserted.
      ingest.transmission(receipt.sendId(),"SENT",null);
     }
    }
   }
  }catch(Exception e){if(running)log.warn("消防TCP会话结束 session={}，原因={}",session,e.toString());}
  finally{sockets.remove(socket);for(String source:sources)connections.closed(source,session);if(stream.pending().length>0)log.warn("消防TCP会话结束残留不完整帧 session={} hex={}",session,Gb26875.hex(stream.pending()));}
 }
 @Override public synchronized void stop(){running=false;if(listener!=null)try{listener.close();}catch(IOException ignored){}for(Socket s:sockets)try{s.close();}catch(IOException ignored){}if(workers!=null){workers.shutdownNow();try{workers.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}}
 @Override public void stop(Runnable callback){stop();callback.run();}
 @Override public boolean isRunning(){return running;}
}
