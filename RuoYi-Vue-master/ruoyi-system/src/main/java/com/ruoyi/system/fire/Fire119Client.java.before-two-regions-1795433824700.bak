package com.ruoyi.system.fire;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;
import javax.crypto.Cipher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** External token is deliberately separate from the RuoYi user session. */
@Component
public class Fire119Client {
 private final Fire119Properties config;private final ObjectMapper json;private final HttpClient http;
 private String cachedToken;private long refreshAt;
 public Fire119Client(Fire119Properties p,ObjectMapper j){
  config=p;json=j;http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 }
 // Keep the vendor example's RSA transformation and 117-byte segmentation.
 public static String encryptSection(String plaintext,String publicKey) throws Exception {
  if(plaintext==null || plaintext.isEmpty())return null;
  byte[] decoded=Base64.getDecoder().decode(publicKey);
  var pubKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
  Cipher cipher=Cipher.getInstance("RSA");cipher.init(Cipher.ENCRYPT_MODE,pubKey);
  byte[] bytes=plaintext.getBytes(StandardCharsets.UTF_8);int inputLen=bytes.length,offLen=0,i=0;
  ByteArrayOutputStream bops=new ByteArrayOutputStream();
  while(inputLen-offLen>0){int length=Math.min(inputLen-offLen,117);byte[] cache=cipher.doFinal(bytes,offLen,length);bops.write(cache);i++;offLen=117*i;}
  bops.close();return Base64.getEncoder().encodeToString(bops.toByteArray());
 }
 private synchronized String token() throws Exception {
  if(cachedToken!=null && System.currentTimeMillis()<refreshAt)return cachedToken;
  Map<String,Object> secretKeyMap=new HashMap<>();secretKeyMap.put("account",config.getAccount());secretKeyMap.put("password",config.getPassword());secretKeyMap.put("timeStamp",System.currentTimeMillis()/1000);
  Map<String,Object> reqMap=new HashMap<>();reqMap.put("orderSource",config.getOrderSource());reqMap.put("secretKey",encryptSection(json.writeValueAsString(secretKeyMap),config.getPublicKey()));
  long requestedAt=System.currentTimeMillis();
  var response=send(HttpRequest.newBuilder(uri("generateToken")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(reqMap),StandardCharsets.UTF_8)));
  if(!"200".equals(String.valueOf(response.get("code"))) || !(response.get("data") instanceof String value) || value.isBlank())throw new IllegalStateException("119认证失败，业务码="+safeCode(response.get("code")));
  cachedToken=(String)response.get("data");refreshAt=requestedAt+Math.max(1,config.getTokenLifetimeSeconds()-60)*1000L;return cachedToken;
 }
 private synchronized void invalidate(String used){if(Objects.equals(cachedToken,used)){cachedToken=null;refreshAt=0;}}
 private URI uri(String path){return URI.create(config.getBaseUrl().replaceAll("/+$","")+"/"+path);}
 private static String safeCode(Object code){String s=String.valueOf(code);return s.matches("[A-Za-z0-9_-]{1,32}")?s:"unknown";}
 @SuppressWarnings("unchecked")
 private Map<String,Object> send(HttpRequest.Builder request) throws Exception {
  var future=http.sendAsync(request.timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds())).header("Accept","application/json").build(),info->new LimitedBody());
  HttpResponse<byte[]> response;
  try {response=future.get(config.getRequestTimeoutSeconds(),java.util.concurrent.TimeUnit.SECONDS);}
  catch(Exception e){future.cancel(true);throw e;}
  if(response.statusCode()==401 || response.statusCode()==403)throw new AuthenticationExpired();
  if(response.statusCode()!=200)throw new IllegalStateException("119 HTTP状态="+response.statusCode());
  try {return json.readValue(response.body(),Map.class);}catch(Exception e){throw new IllegalStateException("119响应不是有效JSON（请检查地址和网关）");}

 }
 @SuppressWarnings("unchecked")
 public Map<String,Object> page(int page) throws Exception {
  for(int attempt=0;attempt<2;attempt++){
   String used=token();Map<String,Object> response;
   try {
    URI address=URI.create(uri("queryParamsXfjqxx")+"?xzqybh="+URLEncoder.encode(config.getRegionCode(),StandardCharsets.UTF_8)+"&pageNum="+page+"&pageSize="+config.getPageSize());
    response=send(HttpRequest.newBuilder(address).GET().header("token",used).header("orderSource",config.getOrderSource()));
    String code=String.valueOf(response.get("code"));
    if(code.equals("401")||code.equals("403"))throw new AuthenticationExpired();
    if(!code.equals("200"))throw new IllegalStateException("119查询失败，业务码="+safeCode(response.get("code")));
   }catch(AuthenticationExpired e){invalidate(used);if(attempt==0)continue;throw new IllegalStateException("119认证刷新后仍失败");}
   if(!(response.get("data") instanceof Map))throw new IllegalStateException("119分页data结构异常");
   return (Map<String,Object>)response.get("data");
  }
  throw new IllegalStateException("119查询未完成");
 }
 private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
  private final java.util.concurrent.CompletableFuture<byte[]> body=new java.util.concurrent.CompletableFuture<>();
  private final ByteArrayOutputStream output=new ByteArrayOutputStream();
  private java.util.concurrent.Flow.Subscription subscription;
  public java.util.concurrent.CompletionStage<byte[]> getBody(){return body;}
  public void onSubscribe(java.util.concurrent.Flow.Subscription s){subscription=s;s.request(1);}
  public void onNext(List<java.nio.ByteBuffer> buffers){
   for(var buffer:buffers){
    if((long)output.size()+buffer.remaining()>4*1024*1024){subscription.cancel();body.completeExceptionally(new IllegalStateException("119响应超过4MiB限制"));return;}
    byte[] bytes=new byte[buffer.remaining()];buffer.get(bytes);output.writeBytes(bytes);
   }
   subscription.request(1);
  }
  public void onError(Throwable t){body.completeExceptionally(t);}
  public void onComplete(){body.complete(output.toByteArray());}
 }
 private static class AuthenticationExpired extends Exception {}
}
