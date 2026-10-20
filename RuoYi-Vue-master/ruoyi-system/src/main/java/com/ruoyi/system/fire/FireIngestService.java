package com.ruoyi.system.fire;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import com.ruoyi.system.mapper.FireAccessMapper;

@Service
public class FireIngestService {
 public record Receipt(String source,Long receiveId,Long sendId,byte[] reply) {}
 private final FireAccessMapper mapper;private final ObjectMapper json;
 public FireIngestService(FireAccessMapper m,ObjectMapper j){mapper=m;json=j;}
 private static Long number(Object n){return n==null?null:((Number)n).longValue();}
 private String json(Object v){try{return json.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException(e);}}
 private static String hash(String text){try{return Gb26875.hex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private Map<String,Object> message(Long gateway,String session,String ip,String direction,byte[] raw,Gb26875.Frame frame,String status,String error,Object parsed,String tx) {
  Map<String,Object> m=new HashMap<>();m.put("gatewayId",gateway);m.put("session",session);m.put("peerIp",ip);m.put("direction",direction);
  m.put("sequence",frame==null?null:frame.sequence());m.put("command",frame==null?null:frame.command());m.put("receivedAt",new Date());
  m.put("deviceTime",frame==null?null:Gb26875.format(frame.time()));m.put("raw",Gb26875.hex(raw));m.put("parseStatus",status);
  m.put("error",error);m.put("parsed",parsed==null?null:json(parsed));m.put("transmissionStatus",tx);return m;
 }
 @Transactional(rollbackFor=Exception.class)
 public void rawChunk(String session,String ip,byte[] raw) {
  mapper.insertMessage(message(mapper.sessionGateway(session),session,ip,"RX",raw,null,"RAW_CHUNK",null,null,null));
 }
 /** Transaction proxy returns only after commit. Caller must not send before this returns. */
 @Transactional(rollbackFor=Exception.class)
 public Receipt receive(Gb26875.Packet packet,String session,String ip) {
  byte[] raw=packet.raw();Gb26875.Frame frame=packet.frame();
  String source=raw.length>=24 && raw[0]==64 && raw[1]==64?Gb26875.hex(Arrays.copyOfRange(raw,12,18)):null;
  Map<String,Object> gateway=source==null?null:mapper.gatewayForUpdate(source);
  boolean allowed=gateway!=null && "0".equals(gateway.get("enabled"))
    && ip.equals(gateway.get("allowedIp")) && raw.length>=27
    && gateway.get("centerAddress").equals(Gb26875.hex(Arrays.copyOfRange(raw,18,24)));
  Long gatewayId=gateway==null?null:number(gateway.get("gatewayId"));
  List<Map<String,Object>> objects=List.of();String status="INVALID",error=packet.error();
  int replyCommand=0;
  if(!allowed) {status="REJECTED";error="未登记、停用或来源IP/中心协议地址不匹配";}
  else if(Gb26875.u(raw[4])!=1) {status="UNSUPPORTED";error="不支持的协议主版本";}
  else if(frame==null) {replyCommand=Gb26875.u(raw[26])==2?6:0;}
  else if(frame.command()!=2) {status="IGNORED";error="仅接收发送数据命令，不主动执行控制或查询";}
  else {
   try {objects=Gb26875.objects(frame);status=frame.data().length==0?"EMPTY":"PARSED";replyCommand=3;}
   catch(UnsupportedOperationException e){status="UNSUPPORTED";error=e.getMessage();replyCommand=3;}
   catch(IllegalArgumentException e){status="MALFORMED";error=e.getMessage();replyCommand=6;}
  }
  Map<String,Object> rx=message(gatewayId,session,ip,"RX",raw,frame,status,error,objects,null);
  mapper.insertMessage(rx);Long rxId=number(rx.get("messageId"));
  if(allowed) mapper.bindRawSession(session,gatewayId);
  if(allowed && frame!=null && Gb26875.u(raw[4])==1) {
   Map<String,Object> touch=new HashMap<>();touch.put("gatewayId",gatewayId);touch.put("receivedAt",rx.get("receivedAt"));mapper.touchGateway(touch);
  }
  if("PARSED".equals(status)) for(Map<String,Object> o:objects) {
   String eventTime=(String)o.get("eventTime");Long hostId=null;
   Map<String,Object> host=null;
   if(o.containsKey("systemAddress")) {
    host=new HashMap<>(o);host.put("gatewayId",gatewayId);host.put("receivedAt",rx.get("receivedAt"));
    host.put("hostName","消防系统 "+o.get("systemType")+"/"+o.get("systemAddress"));
    // A component snapshot cannot overwrite the whole host's state.
    host.put("bits",Integer.valueOf(1).equals(o.get("type"))?o.get("statusBits"):null);
    host.put("stateName",host.get("bits")==null?null:o.get("name"));host.put("stateTime",eventTime);
    host.put("version",o.get("softwareVersion"));mapper.upsertHost(host);
    hostId=number(mapper.host(host).get("hostId"));
   }
   String objectKey=o.containsKey("systemAddress")?"system:"+o.get("systemType")+":"+o.get("systemAddress")+(o.containsKey("componentAddress")?":component:"+o.get("componentType")+":"+o.get("componentAddress"):""):"gateway:"+source;
   Map<String,Object> event=new HashMap<>(o);
   event.put("eventId",hash(source+"|"+o.get("type")+"|"+o.get("raw")));
   event.put("gatewayId",gatewayId);event.put("hostId",hostId);event.put("messageId",rxId);event.put("objectKey",objectKey);
   event.put("deviceTime",eventTime);event.put("receivedAt",rx.get("receivedAt"));event.put("details",json(o));
   LocalDateTime device=eventTime==null?null:LocalDateTime.parse(eventTime.replace(' ','T'));
   boolean warning=device==null || Math.abs(Duration.between(device,LocalDateTime.now(ZoneId.of("Asia/Shanghai"))).getSeconds())>86400;
   event.put("clockWarning",warning?1:0);mapper.insertEvent(event);
   if(Integer.valueOf(21).equals(o.get("type"))) {
    event.put("bits",o.get("statusBits"));mapper.updateGatewayState(event);
   }
  }
  if(replyCommand==0)return new Receipt(allowed?source:null,rxId,null,null);
  byte[] reply=Gb26875.response(raw,replyCommand,LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
  Map<String,Object> tx=message(gatewayId,session,ip,"TX",reply,Gb26875.decode(reply),"REPLY",null,null,"QUEUED");
  mapper.insertMessage(tx);
  return new Receipt(source,rxId,number(tx.get("messageId")),reply);
 }
 public void transmission(Long id,String status,String error){mapper.updateTransmission(id,status,error);}
}
