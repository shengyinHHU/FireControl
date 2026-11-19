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
  }catch(IOException e){if(listener!=null)try{listener.close();}catch(IOException ignored){}throw new IllegalStateException("消防TCP监听启动失败 ["+config.getBindAddress()+":"+config.getPort()+"]：请确认监听地址属于本机（可使用0.0.0.0监听所有IPv4网卡），且端口未被占用",e);}
 }
 private void accept(){
  while(running)try{
   Socket socket=listener.accept();socket.setSoTimeout(1000);sockets.add(socket);
   try{workers.execute(()->session(socket));}catch(RejectedExecutionException e){sockets.remove(socket);socket.close();log.warn("消防TCP连接数达到上限");}
  }catch(IOException e){if(running)log.error("消防TCP接收连接失败",e);}
 }
 private void session(Socket socket){
  try{FireDeviceSession.run(socket,socket.getInetAddress().getHostAddress(),()->running,config,ingest,connections);}
  finally{sockets.remove(socket);}
 }
 @Override public synchronized void stop(){running=false;if(listener!=null)try{listener.close();}catch(IOException ignored){}for(Socket s:sockets)try{s.close();}catch(IOException ignored){}if(workers!=null){workers.shutdownNow();try{workers.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}}
 @Override public void stop(Runnable callback){stop();callback.run();}
 @Override public boolean isRunning(){return running;}
}
