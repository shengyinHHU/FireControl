package com.ruoyi.web.controller.fire;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.system.fire.FireAlarmUpdates;
/** Owns one emitter's terminal state across timeout, disconnect and broker expiry races. */
public final class FireAlarmEmitter implements FireAlarmUpdates.Sink {
 private final SseEmitter emitter;
 private final AtomicBoolean terminal=new AtomicBoolean();
 public FireAlarmEmitter(SseEmitter e){emitter=e;}
 public void unusable(){terminal.set(true);}
 @Override public void send(String event)throws Exception {
  if(terminal.get())throw new IOException("SSE connection already closed");
  try{emitter.send(event.equals("heartbeat")?SseEmitter.event().comment("keepalive"):SseEmitter.event().name(event).data("refresh"));}
  catch(Exception e){terminal.set(true);throw e;}
 }
 @Override public void close(){
  if(!terminal.compareAndSet(false,true))return;
  try{emitter.complete();}catch(RuntimeException e){/* Already closed by the container; never retry writing this response. */}
 }
}
