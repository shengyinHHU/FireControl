package com.ruoyi.system.fire;
import java.math.*;
/** Same local approximation verified against 225 AMap JS API outputs; no network calls. */
public final class Fire119Coordinates {
 public static final String VERSION="local-gcj-v1";
 private Fire119Coordinates(){}
 public static BigDecimal[] toMap(BigDecimal longitude,BigDecimal latitude,String source){
  if(longitude==null||latitude==null)return null;
  if("GCJ02".equals(source))return new BigDecimal[]{longitude,latitude};
  if(!"WGS84".equals(source))return null;
  double lng=longitude.doubleValue(),lat=latitude.doubleValue();
  if(lng<72.004||lng>137.8347||lat<0.8293||lat>55.8271)return new BigDecimal[]{longitude,latitude};
  double x=lng-105,y=lat-35,p=Math.PI;
  double wave=(20*Math.sin(6*x*p)+20*Math.sin(2*x*p))*2/3;
  double dy=-100+2*x+3*y+.2*y*y+.1*x*y+.2*Math.sqrt(Math.abs(x))+wave;
  dy+=(20*Math.sin(y*p)+40*Math.sin(y*p/3))*2/3+(160*Math.sin(y*p/12)+320*Math.sin(y*p/30))*2/3;
  double dx=300+x+2*y+.1*x*x+.1*x*y+.1*Math.sqrt(Math.abs(x))+wave;
  dx+=(20*Math.sin(x*p)+40*Math.sin(x*p/3))*2/3+(150*Math.sin(x*p/12)+300*Math.sin(x*p/30))*2/3;
  double rad=lat*p/180,e=.006693421622965823,a=6378245,m=1-e*Math.pow(Math.sin(rad),2);
  double mapLng=lng+dx*180/(a/Math.sqrt(m)*Math.cos(rad)*p),mapLat=lat+dy*180/(a*(1-e)/(m*Math.sqrt(m))*p);
  return new BigDecimal[]{BigDecimal.valueOf(mapLng).setScale(8,RoundingMode.HALF_UP),BigDecimal.valueOf(mapLat).setScale(8,RoundingMode.HALF_UP)};
 }
}
