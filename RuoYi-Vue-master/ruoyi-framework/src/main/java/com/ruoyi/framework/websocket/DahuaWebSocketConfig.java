package com.ruoyi.framework.websocket;

import java.util.concurrent.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import tools.jackson.databind.ObjectMapper;
import com.ruoyi.system.fire.dahua.DahuaProperties;
import jakarta.annotation.PreDestroy;

@Configuration
@EnableWebSocket
public class DahuaWebSocketConfig implements WebSocketConfigurer {
 private final DahuaTalkService calls;private final DahuaProperties config;private final ObjectMapper json=new ObjectMapper();
 private final ScheduledExecutorService deadlines=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"dahua-handshake");t.setDaemon(true);return t;});
 public DahuaWebSocketConfig(DahuaTalkService s,DahuaProperties c){calls=s;config=c;}
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(new Handler(),"/fire/video/dahua/socket").setAllowedOrigins(config.getAllowedOrigins().toArray(String[]::new));}
 private class Handler extends AbstractWebSocketHandler {
  @Override public void afterConnectionEstablished(WebSocketSession s){s.setBinaryMessageSizeLimit(640);s.setTextMessageSizeLimit(512);s.getAttributes().put("deadline",deadlines.schedule(()->{if(!s.getAttributes().containsKey("call"))close(s);},5,TimeUnit.SECONDS));}
  @Override protected void handleTextMessage(WebSocketSession s,TextMessage m)throws Exception{
   var node=json.readTree(m.getPayload());var call=(DahuaTalkService.Session)s.getAttributes().get("call");
   if(call==null){if(!"auth".equals(node.path("type").asText())){close(s);return;}try{call=calls.authenticate(node.path("ticket").asText(),s);s.getAttributes().put("call",call);((Future<?>)s.getAttributes().get("deadline")).cancel(false);}catch(Exception e){close(s);}}
   else if("ping".equals(node.path("type").asText()))call.heartbeat();else if("stop".equals(node.path("type").asText()))call.finish(null);else call.finish("无效控制消息");
  }
  @Override protected void handleBinaryMessage(WebSocketSession s,BinaryMessage m){var call=(DahuaTalkService.Session)s.getAttributes().get("call");if(call==null){close(s);return;}byte[] bytes=new byte[m.getPayloadLength()];m.getPayload().get(bytes);call.audio(bytes);}
  @Override public void afterConnectionClosed(WebSocketSession s,CloseStatus status){var future=(Future<?>)s.getAttributes().get("deadline");if(future!=null)future.cancel(false);var call=(DahuaTalkService.Session)s.getAttributes().get("call");if(call!=null)call.finish(null);}
  @Override public void handleTransportError(WebSocketSession s,Throwable error){close(s);}
 }
 private static void close(WebSocketSession s){try{s.close(CloseStatus.POLICY_VIOLATION);}catch(Exception ignored){}}
 @PreDestroy public void destroy(){deadlines.shutdownNow();}
}
