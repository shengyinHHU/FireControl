package com.ruoyi.system.fire;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.fire.video.*;

@Service
public class FireVideoClient {
 private final FireVideoProperties config; private final ObjectMapper json; private final FireVideoMedia media;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 private String cachedToken; private long refreshAt;
 // Preview is restricted to devices actually returned by this configured enterprise account.
 private final Map<String,Long> knownDevices=new java.util.concurrent.ConcurrentHashMap<>();
 public FireVideoClient(FireVideoProperties c,ObjectMapper j,FireVideoMedia m){config=c;json=j;media=m;}
 private void access(){
  if(!config.isEnabled())throw new ServiceException("视频监控未启用");
  if(!SecurityUtils.isAdmin() && !config.getAllowedUserIds().contains(SecurityUtils.getUserId()))throw new ServiceException("未授权访问此监控账号",403);
 }
 @SuppressWarnings("unchecked")
 private Map<String,Object> call(String path,Map<String,Object> business) throws Exception {
  StringJoiner plain=new StringJoiner("&");business.forEach((k,v)->plain.add(k+"="+v));
  String appId=config.getAppId(),secret=config.getAppSecret();
  if(appId==null||secret==null||config.getEnterpriseUser()==null)throw new ServiceException("视频监控配置不完整");
  String params=XXTea.encrypt(plain.toString(),"UTF-8",ByteFormat.toHex(secret.getBytes(StandardCharsets.UTF_8)));
  String timestamp=Long.toString(System.currentTimeMillis());
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
  String signature=ByteFormat.toHex(mac.doFinal((appId+"3"+params+timestamp+"v1.0").getBytes(StandardCharsets.UTF_8)));
  Map<String,String> form=Map.of("appId",appId,"clientType","3","version","v1.0","timestamp",timestamp,"params",params,"signature",signature);
  StringJoiner body=new StringJoiner("&");form.forEach((k,v)->body.add(k+"="+URLEncoder.encode(v,StandardCharsets.UTF_8)));
  var request=HttpRequest.newBuilder(URI.create("https://vcp.21cn.com"+path)).timeout(Duration.ofSeconds(30))
   .header("apiVersion","2.0").header("Content-Type","application/x-www-form-urlencoded")
   .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
  var response=http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  if(response.statusCode()!=200)throw new ServiceException("视频平台连接失败，HTTP "+response.statusCode());
  Map<String,Object> result=json.readValue(response.body(),Map.class);
  Object code=result.containsKey("code")?result.get("code"):result.get("CODE");
  if(!"0".equals(String.valueOf(code))){
   String message=String.valueOf(result.getOrDefault("msg",result.getOrDefault("MSG","未知错误")));
   throw new ServiceException("视频平台 ["+code+"]："+message);
  }
  return result;
 }
 @SuppressWarnings("unchecked")
 private synchronized String token() throws Exception {
  if(cachedToken!=null && System.currentTimeMillis()<refreshAt)return cachedToken;
  long requested=System.currentTimeMillis();
  Map<String,Object> data=(Map<String,Object>)call("/open/oauth/getAccessToken",Map.of("grantType","vcp_189")).get("data");
  if(data==null||!(data.get("accessToken") instanceof String value)||value.isBlank())throw new ServiceException("视频平台未返回有效令牌");
  cachedToken=value;long expires=Long.parseLong(String.valueOf(data.get("expiresIn")));
  refreshAt=requested+Math.max(1,expires-300)*1000L;return cachedToken;
 }
 private Map<String,Object> business() throws Exception {
  Map<String,Object> b=new LinkedHashMap<>();b.put("accessToken",token());b.put("enterpriseUser",config.getEnterpriseUser());return b;
 }
 public Object regions(Long regionId) throws Exception {
  access();var b=business();b.put("regionId",regionId==null?"":regionId);
  return call("/open/token/device/getReginWithGroupList",b).get("data");
 }
 @SuppressWarnings("unchecked")
 public Object devices(Long regionId,long lastId,int pageSize) throws Exception {
  access();var b=business();if(regionId!=null)b.put("cusRegionId",regionId);
  b.put("lastId",lastId);b.put("pageSize",Math.max(1,Math.min(100,pageSize)));b.put("hasChildDevices",0);
  Object data=call("/open/token/device/getAllDeviceListNew",b).get("data");
  if(data instanceof Map<?,?> page && page.get("list") instanceof List<?> rows){
   long now=System.currentTimeMillis();knownDevices.entrySet().removeIf(e->e.getValue()<now);
   for(Object row:rows)if(row instanceof Map<?,?> device && device.get("deviceCode") instanceof String code)knownDevices.put(code,now+3600000);
  }
  return data;
 }
 @SuppressWarnings("unchecked")
 public Object preview(String code,String protocol) throws Exception {
  if(!Set.of("hls","rtsp","webrtc","flv").contains(protocol))throw new ServiceException("不支持的播放协议");
  access();if(code==null||knownDevices.getOrDefault(code,0L)<System.currentTimeMillis())throw new ServiceException("请先刷新设备列表后再播放");
  var b=business();b.put("deviceCode",code);b.put("mediaType",1);b.put("mute",protocol.equals("flv")?1:0);
  if(protocol.equals("webrtc")){b.put("netType",0);b.put("channel",0);}else if(!protocol.equals("flv"))b.put("supportDomain",1);
  String path=protocol.equals("webrtc")?"/open/token/vpaas/getDeviceMediaWebrtcUrl":"/open/token/cloud/"+(protocol.equals("rtsp")?"getDeviceMediaUrlRtsp":protocol.equals("flv")?"getDeviceMediaUrlFlv":"getDeviceMediaUrlHls");
  Object encrypted=call(path,b).get("data");
  Map<String,Object> decoded;
  if(encrypted instanceof Map<?,?> data)decoded=(Map<String,Object>)data;
  else if(encrypted instanceof String text){
   if(protocol.equals("webrtc") && (text.startsWith("webrtc://")||text.startsWith("rtc://")))decoded=Map.of("url",text);
   else decoded=json.readValue(XXTea.decrypt(text,"UTF-8",ByteFormat.toHex(config.getAppSecret().getBytes(StandardCharsets.UTF_8))),Map.class);
  }else throw new ServiceException("视频平台未返回播放地址");
  Object value=decoded.get("url");
  if(!(value instanceof String url)||!(protocol.equals("webrtc")?(url.startsWith("webrtc://")||url.startsWith("rtc://")):(protocol.equals("hls")||protocol.equals("flv"))?url.startsWith("https://"):url.startsWith("rtsp://")))throw new ServiceException("视频平台未返回有效播放地址");
  Map<String,Object> out=new LinkedHashMap<>();out.put("url",protocol.equals("flv")?media.openFlv(url):protocol.equals("hls")?media.open(url):url);out.put("requiresAuth",protocol.equals("hls")||protocol.equals("flv"));out.put("protocol",protocol);out.put("expireTime",decoded.get("expireTime"));if(protocol.equals("webrtc"))out.put("signalUrl",media.openRtc(url));return out;
 }
}
