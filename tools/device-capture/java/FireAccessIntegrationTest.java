import com.ruoyi.system.fire.*;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.mapper.FireAccessMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import java.sql.*;
import java.net.*;
import java.time.*;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

/** Disposable-database test main. Never uses application datasource configuration. */
public class FireAccessIntegrationTest {
 static int checks=0;
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
 static Connection db;
 static long count(String table)throws Exception{try(var s=db.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
 static byte[] frame(int seq,int type,byte[] object){byte[] base=Gb26875.unhex("404037000102380813090311c80000000000640000000000000002d82323");
  byte[] data=new byte[object.length+2];data[0]=(byte)type;data[1]=1;System.arraycopy(object,0,data,2,object.length);
  byte[] b=new byte[30+data.length];System.arraycopy(base,0,b,0,27);b[2]=(byte)seq;b[3]=(byte)(seq>>8);b[24]=(byte)data.length;b[25]=(byte)(data.length>>8);
  System.arraycopy(data,0,b,27,data.length);int sum=0;for(int i=2;i<b.length-3;i++)sum+=Gb26875.u(b[i]);b[b.length-3]=(byte)sum;b[b.length-2]=35;b[b.length-1]=35;return b;
 }
 static FireIngestService.Receipt ingest(FireIngestService service,byte[] b,String ip){
  Gb26875.Packet p;try{p=new Gb26875.Packet(b,Gb26875.decode(b),null);}catch(Exception e){p=new Gb26875.Packet(b,null,e.getMessage());}
  return service.receive(p,"test-session",ip);
 }

 static void codecChecks() {
  int[] types={1,2,3,4,5,6,7,8,21,24,25,26,28};
  int[] sizes={10,46,16,10,4,3,38,8,7,8,2,1,6};
  for(int i=0;i<types.length;i++) {
   byte[] object=new byte[sizes[i]];
   byte[] b=frame(200+i,types[i],object);
   check(Gb26875.objects(Gb26875.decode(b)).size()==1,"uplink type "+types[i]);
   byte[] invalid=Arrays.copyOf(object,object.length+1);
   boolean failed=false;try{Gb26875.objects(Gb26875.decode(frame(300+i,types[i],invalid)));}catch(IllegalArgumentException e){failed=true;}
   check(failed,"strict object length "+types[i]);
  }
  byte[] sample=Gb26875.unhex("404037000102380813090311c80000000000640000000000000002d82323");
  byte[] noise=new byte[]{0,1,2};
  var stream=new Gb26875.Stream();
  byte[] combined=new byte[noise.length+sample.length*3];System.arraycopy(noise,0,combined,0,noise.length);
  for(int i=0;i<3;i++)System.arraycopy(sample,0,combined,noise.length+i*sample.length,sample.length);
  check(stream.feed(combined).stream().filter(p->p.frame()!=null).count()==3,"noise and concatenated frames");
  byte[] max=new byte[1054];System.arraycopy(sample,0,max,0,27);max[24]=0;max[25]=4;
  for(int i=27;i<max.length-3;i++)max[i]=(byte)(i%2==0?64:35);
  int sum=0;for(int i=2;i<max.length-3;i++)sum+=Gb26875.u(max[i]);max[max.length-3]=(byte)sum;max[max.length-2]=35;max[max.length-1]=35;
  check(new Gb26875.Stream().feed(max).stream().filter(p->p.frame()!=null).count()==1,"max ADU with internal delimiters");
  byte[] reply=Gb26875.response(sample,3,LocalDateTime.of(2026,10,20,17,40,1));
  check(Arrays.equals(Arrays.copyOfRange(reply,12,18),Arrays.copyOfRange(sample,18,24)),"ACK source address swap");
  check(Arrays.equals(Arrays.copyOfRange(reply,18,24),Arrays.copyOfRange(sample,12,18)),"ACK destination address swap");
 }

 public static void main(String[] args)throws Exception {
  codecChecks();
  var broker=new FireAlarmUpdates();
  var permitted=new java.util.concurrent.CopyOnWriteArrayList<String>();var forbidden=new java.util.concurrent.CopyOnWriteArrayList<String>();
  var signal=new java.util.concurrent.CountDownLatch(1);
  Runnable unsubscribe=broker.subscribe(1L,Set.of(1L),new FireAlarmUpdates.Sink(){public void send(String event){permitted.add(event);if(event.equals("alarm-change"))signal.countDown();}public void close(){}});
  broker.subscribe(2L,Set.of(999L),new FireAlarmUpdates.Sink(){public void send(String event){forbidden.add(event);}public void close(){}});
  broker.changed(1L);check(signal.await(2,java.util.concurrent.TimeUnit.SECONDS),"scoped subscriber receives coalesced signal");
  check(!forbidden.contains("alarm-change"),"unauthorized gateway subscribers receive no change signal");
  unsubscribe.run();int permittedBefore=permitted.size();broker.changed(1L);Thread.sleep(350);
  check(permitted.size()==permittedBefore,"unsubscribe removes subscriber");broker.destroy();
  String url="jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
  db=DriverManager.getConnection(url,"root","");
  var ds=new DriverManagerDataSource(url,"root","");
  var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);
  factory.setMapperLocations(new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FireAccessMapper.xml"));
  var template=new SqlSessionTemplate(factory.getObject());
  var mapper=template.getMapper(FireAccessMapper.class);
  // Boot 4 must create the Jackson 3 mapper used by the actual service constructor.
  var context=new org.springframework.context.annotation.AnnotationConfigApplicationContext();
  context.register(org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class,FireIngestService.class);
  java.util.concurrent.atomic.AtomicInteger committedSignals=new java.util.concurrent.atomic.AtomicInteger();
  java.util.concurrent.atomic.AtomicLong visibleAtSignal=new java.util.concurrent.atomic.AtomicLong();
  var updates=new FireAlarmUpdates(){@Override public void changed(Long gateway){
   try{visibleAtSignal.set(count("fire_event"));}catch(Exception e){throw new RuntimeException(e);}committedSignals.incrementAndGet();super.changed(gateway);
  }};
  context.registerBean(FireAlarmUpdates.class,()->updates);
  context.registerBean(FireAccessMapper.class,()->mapper);
  context.refresh();
  check(context.getBean(tools.jackson.databind.ObjectMapper.class)!=null,"Boot Jackson 3 mapper bean");
  var injected=context.getBean(FireIngestService.class);
  check(injected!=null,"Spring injects real FireIngestService constructor");
  var proxy=new TransactionProxyFactoryBean();proxy.setTarget(injected);
  proxy.setTransactionManager(new DataSourceTransactionManager(ds));proxy.setProxyTargetClass(true);
  var tx=new Properties();tx.setProperty("*","PROPAGATION_REQUIRED");proxy.setTransactionAttributes(tx);proxy.afterPropertiesSet();
  var service=(FireIngestService)proxy.getObject();
  var json=new ObjectMapper();
  var lines=Files.readAllLines(Path.of(args[1],"tools/device-capture/captures/20261020_174338_705753/events.jsonl"));
  int captured=0;
  for(String line:lines){var row=json.readTree(line);if(!"frame_received".equals(row.path("event").asText()))continue;
   byte[] b=Gb26875.unhex(row.get("hex").asText());var receipt=ingest(service,b,"10.5.90.49");
   check(receipt.reply()!=null,"captured ACK");var ack=Gb26875.decode(receipt.reply());check(ack.command()==3,"ACK command");
   check(ack.sequence()==Gb26875.decode(b).sequence(),"ACK sequence");service.transmission(receipt.sendId(),"SENT",null);captured++;
   for(int cut=1;cut<b.length;cut++){var stream=new Gb26875.Stream();check(stream.feed(Arrays.copyOfRange(b,0,cut)).isEmpty(),"split wait");check(stream.feed(Arrays.copyOfRange(b,cut,b.length)).stream().filter(p->p.frame()!=null).count()==1,"split assemble");}
  }
  check(captured==8,"8 captured frames");check(count("fire_event")==5,"5 business events");check(count("fire_host")==0,"no invented hosts");
  check(count("fire_protocol_message")==16,"all RX and TX retained");
  try(var s=db.createStatement();var r=s.executeQuery("SELECT status_bits FROM fire_gateway")){r.next();check(r.getInt(1)==0x14,"latest device state");}
  var q=new FireAccessQuery();check(mapper.gateways(q).size()==1,"gateway query");check(mapper.events(q).size()==5,"event query");check(mapper.alarmGatewayIds(q).size()==1,"SSE gateway scope query");
  check(mapper.messages(q).size()==16,"message query");check(mapper.gatewayCount(q)==1,"gateway count");
  q.getParams().put("dataScope"," AND p.dept_id=999999");check(mapper.gateways(q).isEmpty(),"gateway scope");
  check(mapper.events(q).isEmpty(),"event scope");check(mapper.alarmGatewayIds(q).isEmpty(),"SSE scope excludes unauthorized gateway");check(mapper.messages(q).isEmpty(),"raw message scope");
  check(((Number)mapper.eventStats(q).get("eventTotal")).longValue()==0,"statistics scope");q=new FireAccessQuery();
  byte[] tag={1,0,12,20,10,26};
  byte[] status=new byte[7];status[0]=3;System.arraycopy(tag,0,status,1,6);
  byte[] fire=frame(100,21,status);
  check(Gb26875.objects(Gb26875.decode(fire)).get(0).get("kind").equals("fire"),"gateway FIRE bit");
  int signalsBefore=committedSignals.get();ingest(service,fire,"10.5.90.49");
  check(committedSignals.get()==signalsBefore+1&&visibleAtSignal.get()==count("fire_event"),"change notification sees committed rows on separate DB connection");long eventCount=count("fire_event");ingest(service,fire,"10.5.90.49");
  check(count("fire_event")==eventCount,"duplicate event dedup with repeated receipts");
  status[0]=1;byte[] restored=frame(101,21,status);ingest(service,restored,"10.5.90.49");
  try(var s=db.createStatement();var r=s.executeQuery("SELECT status_bits FROM fire_gateway")){r.next();check(r.getInt(1)==1,"recovery snapshot");}
  byte[] old=frame(102,21,new byte[]{0,0,0,0,1,1,17});ingest(service,old,"10.5.90.49");
  try(var s=db.createStatement();var r=s.executeQuery("SELECT status_bits FROM fire_gateway")){r.next();check(r.getInt(1)==1,"old event cannot overwrite snapshot");}
  byte[] hostStatus=frame(103,1,new byte[]{1,1,3,0,1,0,12,20,10,26});ingest(service,hostStatus,"10.5.90.49");
  check(count("fire_host")==1,"actual host discovered");
  check(mapper.hosts(q).size()==1,"host list SQL");check(mapper.hostConnectionCounts(q).size()==1,"online grouping SQL");
  check(Gb26875.objects(Gb26875.decode(hostStatus)).get(0).get("fire").equals(true),"system FIRE bit");
  byte[] component=new byte[46];component[0]=1;component[1]=1;component[2]=25;component[3]=7;component[7]=3;System.arraycopy(tag,0,component,40,6);
  ingest(service,frame(104,2,component),"10.5.90.49");check(count("fire_host")==1,"component not invented host");
  check(Gb26875.objects(Gb26875.decode(frame(104,2,component))).get(0).get("fire").equals(true),"component FIRE bit");
  check(ingest(service,fire,"127.0.0.1").reply()==null,"wrong IP rejected");
  byte[] corrupt=fire.clone();corrupt[corrupt.length-3]^=1;check(Gb26875.decode(ingest(service,corrupt,"10.5.90.49").reply()).command()==6,"checksum NACK");
  byte[] malformed=frame(105,21,new byte[]{1});check(Gb26875.decode(ingest(service,malformed,"10.5.90.49").reply()).command()==6,"malformed object NACK");
  check(Gb26875.decode(ingest(service,frame(106,128,new byte[]{1}),"10.5.90.49").reply()).command()==3,"unknown extension durably captured");
  byte[] ackInput=fire.clone();ackInput[26]=3;int sumAck=0;for(int i=2;i<ackInput.length-3;i++)sumAck+=Gb26875.u(ackInput[i]);ackInput[ackInput.length-3]=(byte)sumAck;
  check(ingest(service,ackInput,"10.5.90.49").reply()==null,"registered source command3 has no ACK loop");
  long before=count("fire_protocol_message");
  try(var s=db.createStatement()){s.execute("CREATE TRIGGER fail_event BEFORE INSERT ON fire_event FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced test failure'");}
  boolean failed=false;try{ingest(service,frame(107,24,new byte[]{4,1,2,0,12,20,10,26}),"10.5.90.49");}catch(Exception e){failed=true;}
  try(var s=db.createStatement()){s.execute("DROP TRIGGER fail_event");}
  check(failed,"persistence failure propagates before reply");
  check(count("fire_protocol_message")==before,"RX and TX rolled back on business failure");
  // Register the notification, then force a later reply INSERT failure: rollback must publish nothing.
  int beforeLateRollback=committedSignals.get();long beforeLateEvents=count("fire_event");
  try(var s=db.createStatement()){s.execute("CREATE TRIGGER fail_reply BEFORE INSERT ON fire_protocol_message FOR EACH ROW BEGIN IF NEW.direction='TX' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced reply rollback'; END IF; END");}
  boolean lateFailed=false;try{ingest(service,frame(118,24,new byte[]{5,1,2,0,12,20,10,26}),"10.5.90.49");}catch(Exception e){lateFailed=true;}
  try(var s=db.createStatement()){s.execute("DROP TRIGGER fail_reply");}
  check(lateFailed&&count("fire_event")==beforeLateEvents&&committedSignals.get()==beforeLateRollback,"registered afterCommit notification is discarded on later transaction rollback");
  // Local-only, bounded lifecycle test. Actual device and port 2000 are never used.
  try(var s=db.createStatement()){s.execute("UPDATE fire_gateway SET allowed_ip='127.0.0.1'");}
  var props=new FireDeviceProperties();props.setEnabled(true);props.setBindAddress("127.0.0.1");props.setIdleSeconds(5);
  int port;try(var probe=new java.net.ServerSocket(0)){port=probe.getLocalPort();}props.setPort(port);
  var connections=new FireConnections(props);var tcp=new FireTcpServer(props,service,connections);
  try {tcp.start();try(var socket=new Socket("127.0.0.1",port)){socket.setSoTimeout(3000);socket.getOutputStream().write(fire);byte[] reply=socket.getInputStream().readNBytes(30);check(Gb26875.decode(reply).command()==3,"Java socket ACK after commit");check(connections.online("c80000000000"),"connection online");}}
  finally{tcp.stop();}
  check(!tcp.isRunning(),"listener stopped");
  try(var s=db.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM fire_protocol_message WHERE parse_status='RAW_CHUNK' AND gateway_id IS NOT NULL")){r.next();check(r.getInt(1)>0,"actual TCP chunks retained and bound");}
  context.close();
  db.close();
  System.out.println("FIRE_ACCESS_INTEGRATION_OK checks="+checks);
 }
}
