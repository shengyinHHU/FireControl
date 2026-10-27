import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import javax.net.ssl.*;
import com.ruoyi.system.fire.*;
import com.ruoyi.system.fire.video.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import tools.jackson.databind.ObjectMapper;
public class VideoTalkbackCheck {
 static void check(boolean condition){if(!condition)throw new AssertionError();}
 static void denied(Throwing action)throws Exception {try{action.run();throw new AssertionError("Unexpected acceptance");}catch(ServiceException expected){}}
 interface Throwing{void run()throws Exception;}
 public static void main(String[] args)throws Exception {
  var config=new FireVideoProperties();config.setAppId("fixture-id");config.setAppSecret("fixture-secret");config.setEnterpriseUser("fixture-owner");
  var user=new LoginUser();user.setUserId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
  var client=new FireVideoClient(config,new ObjectMapper(),new FireVideoMedia());var http=new FakeHttp();
  var field=FireVideoClient.class.getDeclaredField("http");field.setAccessible(true);field.set(client,http);
  denied(()->client.talkback("unknown"));check(http.calls==0);
  var known=FireVideoClient.class.getDeclaredField("knownDevices");known.setAccessible(true);((Map)known.get(client)).put("fixture-device",System.currentTimeMillis()+60000);
  user.setUserId(2L);denied(()->client.talkback("fixture-device"));check(http.calls==0);user.setUserId(1L);
  config.setEnabled(false);denied(()->client.talkback("fixture-device"));check(http.calls==0);config.setEnabled(true);
  var result=client.talkback("fixture-device");check(http.calls==2);check(result.keySet().equals(Set.of("wss","token","sessionId")));check(result.get("sessionId").equals("42"));
  for(String bad:List.of("ws://voice.ctseelink.cn","wss://127.0.0.1:8935","wss://user:pass@voice.ctseelink.cn","https://voice.ctseelink.cn"))denied(()->FireVideoClient.voiceTicket(Map.of("wss",bad,"token","fixture","sessionId",42)));
  denied(()->FireVideoClient.voiceTicket(Map.of("wss","wss://voice.ctseelink.cn","sessionId",42)));
  System.out.println("PASS: account/known-device/disabled guards; official signed form, XXTea params, domain=1, netType=0; safe response; insecure/IP/credential URLs rejected. No live API or database used.");
 }
 static class FakeHttp extends HttpClient {
  int calls;
  @Override public <T>HttpResponse<T> send(HttpRequest request,HttpResponse.BodyHandler<T> handler)throws java.io.IOException,InterruptedException {
   calls++;var output=new java.io.ByteArrayOutputStream();
   request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}public void onNext(ByteBuffer b){byte[] bytes=new byte[b.remaining()];b.get(bytes);output.writeBytes(bytes);}public void onError(Throwable t){throw new AssertionError(t);}public void onComplete(){}});
   var form=new HashMap<String,String>();for(String pair:output.toString(StandardCharsets.UTF_8).split("&")){String[] kv=pair.split("=",2);form.put(kv[0],URLDecoder.decode(kv[1],StandardCharsets.UTF_8));}
   check(request.method().equals("POST"));check(request.headers().firstValue("Content-Type").orElse("").equals("application/x-www-form-urlencoded"));check(form.get("version").equals("v1.0"));check(form.get("signature").length()==64);
   String body;
   if(request.uri().getPath().equals("/open/oauth/getAccessToken"))body="{\"code\":0,\"data\":{\"accessToken\":\"ACCOUNT_ONLY\",\"expiresIn\":3600}}";
   else {
    check(request.uri().getPath().equals("/open/token/vpaas/voice/talkback"));
    String plain;try{plain=XXTea.decrypt(form.get("params"),"UTF-8",ByteFormat.toHex("fixture-secret".getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new AssertionError(e);}
    check(plain.equals("accessToken=ACCOUNT_ONLY&enterpriseUser=fixture-owner&deviceCode=fixture-device&domain=1&netType=0"));
    body="{\"code\":0,\"data\":{\"wss\":\"wss://voice.ctseelink.cn:8935?sign=fixture\",\"token\":\"VOICE_ONLY\",\"sessionId\":42}}";
   }
   final String text=body;
   return (HttpResponse<T>)new HttpResponse<String>(){public int statusCode(){return 200;}public HttpRequest request(){return request;}public Optional<HttpResponse<String>> previousResponse(){return Optional.empty();}public HttpHeaders headers(){return HttpHeaders.of(Map.of(),(a,b)->true);}public String body(){return text;}public Optional<SSLSession> sslSession(){return Optional.empty();}public URI uri(){return request.uri();}public HttpClient.Version version(){return Version.HTTP_1_1;}};
  }
  public Optional<java.net.CookieHandler> cookieHandler(){return Optional.empty();}public Optional<Duration> connectTimeout(){return Optional.empty();}public Redirect followRedirects(){return Redirect.NEVER;}public Optional<java.net.ProxySelector> proxy(){return Optional.empty();}public SSLContext sslContext(){try{return SSLContext.getDefault();}catch(Exception e){throw new RuntimeException(e);}}public SSLParameters sslParameters(){return new SSLParameters();}public Optional<java.net.Authenticator> authenticator(){return Optional.empty();}public Version version(){return Version.HTTP_1_1;}public Optional<Executor> executor(){return Optional.empty();}
  public <T>CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r,HttpResponse.BodyHandler<T> h){throw new UnsupportedOperationException();}public <T>CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r,HttpResponse.BodyHandler<T> h,HttpResponse.PushPromiseHandler<T> p){throw new UnsupportedOperationException();}
 }
}
