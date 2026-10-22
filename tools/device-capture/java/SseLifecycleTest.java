import com.ruoyi.system.fire.FireAlarmUpdates;
import com.ruoyi.web.controller.fire.FireAlarmEmitter;
import com.ruoyi.framework.web.exception.GlobalExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import java.io.IOException;
import jakarta.servlet.http.*;

public class SseLifecycleTest {
 static int checks;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
 static class FakeEmitter extends SseEmitter {
  int closes;boolean broken,closeBroken;
  @Override public void send(SseEmitter.SseEventBuilder e)throws IOException{if(broken)throw new AsyncRequestNotUsableException("fixture disconnect");}
  @Override public void complete(){closes++;if(closeBroken)throw new IllegalStateException("fixture close failure");}
 }
 static Object defaultValue(Class<?> type){if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;}
 public static void main(String[] args)throws Exception {
  var emitter=new FakeEmitter();var sink=new FireAlarmEmitter(emitter);sink.close();sink.close();check(emitter.closes==1,"normal completion happens once");
  emitter=new FakeEmitter();sink=new FireAlarmEmitter(emitter);sink.unusable();sink.close();check(emitter.closes==0,"disconnect callback does not re-complete response");
  emitter=new FakeEmitter();emitter.broken=true;sink=new FireAlarmEmitter(emitter);boolean failed=false;
  try{sink.send("alarm-change");}catch(AsyncRequestNotUsableException e){failed=true;}
  sink.close();check(failed&&emitter.closes==0,"failed write marks response unusable and never completes again");
  emitter=new FakeEmitter();emitter.closeBroken=true;sink=new FireAlarmEmitter(emitter);sink.close();sink.close();check(emitter.closes==1,"close failure is contained and not retried");

  AtomicLong clock=new AtomicLong(1000000);var broker=new FireAlarmUpdates(clock::get);
  AtomicInteger invalidCloses=new AtomicInteger();CountDownLatch badWrite=new CountDownLatch(1);
  broker.subscribe(1L,Set.of(1L),new FireAlarmUpdates.Sink(){public void send(String e)throws Exception{if(e.equals("alarm-change")){badWrite.countDown();throw new IOException("fixture disconnected");}}public void close(){invalidCloses.incrementAndGet();throw new IllegalStateException("must not close after write failure");}});
  broker.changed(1L);check(badWrite.await(2,TimeUnit.SECONDS),"broken client write exercised");Thread.sleep(100);check(invalidCloses.get()==0,"broker removes failed sender without calling close");
  AtomicInteger expiryCloses=new AtomicInteger();CountDownLatch expired=new CountDownLatch(1);
  broker.subscribe(2L,Set.of(2L),new FireAlarmUpdates.Sink(){public void send(String e){}public void close(){expiryCloses.incrementAndGet();expired.countDown();throw new IllegalStateException("fixture expiry close failure");}});
  clock.addAndGet(60001);check(expired.await(2,TimeUnit.SECONDS),"expiry close failure exercised");
  CountDownLatch healthy=new CountDownLatch(1);broker.subscribe(3L,Set.of(3L),new FireAlarmUpdates.Sink(){public void send(String e){if(e.equals("alarm-change"))healthy.countDown();}public void close(){}});
  broker.changed(3L);check(healthy.await(2,TimeUnit.SECONDS),"publisher survives failed sender and failed expiry close");check(expiryCloses.get()==1,"expired subscription closed once");
  broker.subscribe(4L,Set.of(),new FireAlarmUpdates.Sink(){public void send(String e){}public void close(){throw new IllegalStateException("fixture shutdown close failure");}});
  broker.destroy();broker.destroy();check(true,"shutdown is idempotent despite close failures");

  var context=new org.springframework.context.annotation.AnnotationConfigApplicationContext();context.register(GlobalExceptionHandler.class);context.refresh();
  try{
   var resolver=new ExceptionHandlerExceptionResolver();resolver.setApplicationContext(context);resolver.afterPropertiesSet();
   Map<String,Object> attributes=new HashMap<>();AtomicInteger responseWrites=new AtomicInteger();
   var request=(HttpServletRequest)java.lang.reflect.Proxy.newProxyInstance(SseLifecycleTest.class.getClassLoader(),new Class[]{HttpServletRequest.class},(proxy,method,a)->switch(method.getName()){
    case "getRequestURI","getServletPath" -> "/fire/alarms/stream";
    case "getMethod" -> "GET";
    case "getAttribute" -> attributes.get(a[0]);
    case "setAttribute" -> {attributes.put((String)a[0],a[1]);yield null;}
    case "removeAttribute" -> {attributes.remove(a[0]);yield null;}
    case "getLocale" -> Locale.CHINA;
    case "getLocales" -> Collections.enumeration(List.of(Locale.CHINA));
    default -> defaultValue(method.getReturnType());
   });
   var response=(HttpServletResponse)java.lang.reflect.Proxy.newProxyInstance(SseLifecycleTest.class.getClassLoader(),new Class[]{HttpServletResponse.class},(proxy,method,a)->switch(method.getName()){
    case "getContentType" -> "text/event-stream";
    case "isCommitted" -> true;
    case "getWriter","getOutputStream","flushBuffer","sendError" -> {responseWrites.incrementAndGet();throw new AssertionError("Cannot write disconnected response");}
    default -> defaultValue(method.getReturnType());
   });
   var resolved=resolver.resolveException(request,response,null,new AsyncRequestNotUsableException("fixture async disconnect"));
   check(resolved!=null&&resolved.isEmpty()&&responseWrites.get()==0,"actual Spring exception resolver handles disconnected SSE without AjaxResult conversion or writing");
   check(context.getBean(GlobalExceptionHandler.class).handleException(new Exception("fixture business error"),request)!=null,"ordinary exceptions still return AjaxResult");
  }finally{context.close();}
  System.out.println("SSE_LIFECYCLE_OK checks="+checks);
 }
}
