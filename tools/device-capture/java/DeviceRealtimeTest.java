import com.ruoyi.system.fire.*;
import com.ruoyi.system.mapper.FireAccessMapper;
import com.ruoyi.web.controller.fire.FireDeviceStreamController;
import com.ruoyi.system.service.FireAccessService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** No deployment services: deterministic scope, lifecycle, presence and transaction callback checks. */
public class DeviceRealtimeTest {
 static int checks;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 static void flush(FireAlarmUpdates broker)throws Exception{var method=FireAlarmUpdates.class.getDeclaredMethod("flush");method.setAccessible(true);method.invoke(broker);}
 public static void main(String[] args)throws Exception{
  AtomicLong clock=new AtomicLong(1000000);
  var broker=new FireAlarmUpdates(clock::get);
  AtomicReference<Set<Long>> scope=new AtomicReference<>(Set.of(1L));
  var allowed=new CopyOnWriteArrayList<String>();var forbidden=new CopyOnWriteArrayList<String>();
  try{
   Runnable unsubscribe=broker.subscribeDevices(1L,scope::get,new FireAlarmUpdates.Sink(){public void send(String event){allowed.add(event);}public void close(){}});
   broker.subscribeDevices(2L,()->Set.of(99L),new FireAlarmUpdates.Sink(){public void send(String event){forbidden.add(event);}public void close(){}});
   broker.deviceChanged(1L);flush(broker);check(allowed.contains("device-change")&&!forbidden.contains("device-change"),"device hints respect gateway scope");
   scope.set(Set.of(1L,2L));int before=allowed.size();broker.deviceChanged(2L);flush(broker);check(allowed.size()>before,"newly registered device is included without reconnect");
   scope.set(Set.of());before=allowed.size();broker.deviceChanged(1L);flush(broker);check(allowed.size()>before,"ownership removal or deletion refreshes previously visible rows");
   scope.set(Set.of(1L));flush(broker);
   TransactionSynchronizationManager.initSynchronization();broker.deviceChangedAfterCommit(1L);var rollback=TransactionSynchronizationManager.getSynchronizations();TransactionSynchronizationManager.clearSynchronization();
   before=allowed.size();rollback.forEach(s->s.afterCompletion(1));flush(broker);check(allowed.size()==before,"rollback does not publish device update");
   TransactionSynchronizationManager.initSynchronization();broker.deviceChangedAfterCommit(1L);var committed=TransactionSynchronizationManager.getSynchronizations();TransactionSynchronizationManager.clearSynchronization();
   committed.forEach(s->s.afterCommit());flush(broker);check(allowed.size()>before,"commit publishes device update");
   unsubscribe.run();before=allowed.size();broker.deviceChanged(1L);flush(broker);check(allowed.size()==before,"device unsubscribe removes listener");

   AtomicInteger presenceHints=new AtomicInteger();var presenceUpdates=new FireAlarmUpdates(){@Override public void deviceChanged(Long id){presenceHints.incrementAndGet();}};
   try{
    var properties=new FireDeviceProperties();properties.setOnlineSeconds(10);var connections=new FireConnections(properties);
    check(connections.online("10.0.0.1",java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"))),"JDBC LocalDateTime presence uses the server's Asia/Shanghai time policy");
    AtomicReference<Date> lastSeen=new AtomicReference<>(new Date());
    var mapper=(FireAccessMapper)java.lang.reflect.Proxy.newProxyInstance(DeviceRealtimeTest.class.getClassLoader(),new Class[]{FireAccessMapper.class},(proxy,method,a)->{
     if(method.getName().equals("gatewayPresence"))return List.of(Map.of("id",1L,"ip","10.0.0.1","receivedAt",lastSeen.get()));
     throw new UnsupportedOperationException(method.getName());
    });
    var monitor=new FireDevicePresenceMonitor(mapper,connections,presenceUpdates);monitor.check();
    lastSeen.set(new Date(System.currentTimeMillis()-11000));monitor.check();check(presenceHints.get()==1,"silent device crossing configured timeout publishes offline change");
    monitor.check();check(presenceHints.get()==1,"unchanged offline device produces no repeated hints");
    lastSeen.set(new Date());monitor.check();check(presenceHints.get()==2,"restored traffic publishes online change");
   }finally{presenceUpdates.destroy();}

   FireAssetIntegrationTest.login(2,100,"1");
   var nullMapper=(FireAccessMapper)java.lang.reflect.Proxy.newProxyInstance(DeviceRealtimeTest.class.getClassLoader(),new Class[]{FireAccessMapper.class},(proxy,method,a)->List.of());
   var controller=new FireDeviceStreamController(broker,new FireAccessService(nullMapper,new FireConnections(new FireDeviceProperties())));
   var response=(jakarta.servlet.http.HttpServletResponse)java.lang.reflect.Proxy.newProxyInstance(DeviceRealtimeTest.class.getClassLoader(),new Class[]{jakarta.servlet.http.HttpServletResponse.class},(proxy,method,a)->null);
   var stream=controller.stream("gateways",response);check(stream!=null,"gateway-only user can establish device stream without alarm permission");
   boolean denied=false;try{controller.stream("alarms",response);}catch(org.springframework.security.access.AccessDeniedException e){denied=true;}
   check(denied,"gateway-only role cannot upgrade to alarm subscription");
   stream.complete();
  }finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();broker.destroy();}
  System.out.println("DEVICE_REALTIME_OK checks="+checks);
 }
}
