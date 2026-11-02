package com.ruoyi.system.fire;

import java.math.*;
import java.util.*;
import com.ruoyi.common.exception.ServiceException;

/** Coordinate validation shared by spreadsheet and single-device maintenance. */
public final class FireVideoCoordinates {
 private FireVideoCoordinates(){}
 public static String system(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replace("-","").replace("_","");}
 public static BigDecimal number(String value){
  try { if(value==null||!value.trim().matches("[+-]?\\d{1,3}(\\.\\d{1,10})?"))throw new IllegalArgumentException();return new BigDecimal(value.trim()); }
  catch(Exception e){throw new ServiceException("经纬度须为十进制度数，例如118.753000、32.229000");}
 }
 public static BigDecimal[] convert(BigDecimal lng,BigDecimal lat,String crs){
  if(lng==null||lat==null||lng.abs().compareTo(new BigDecimal(180))>0||lat.abs().compareTo(new BigDecimal(90))>0||(lng.signum()==0&&lat.signum()==0))throw new ServiceException("经纬度超出范围或为0,0");
  BigDecimal[] out;
  if("BD09".equals(crs)){
   double x=lng.doubleValue()-.0065,y=lat.doubleValue()-.006,p=Math.PI*3000/180;
   double z=Math.sqrt(x*x+y*y)-.00002*Math.sin(y*p),theta=Math.atan2(y,x)-.000003*Math.cos(x*p);
   out=new BigDecimal[]{BigDecimal.valueOf(z*Math.cos(theta)),BigDecimal.valueOf(z*Math.sin(theta))};
  }else out=Fire119Coordinates.toMap(lng,lat,crs);
  if(out==null)throw new ServiceException("有坐标时必须填写GCJ-02、WGS84或BD-09坐标系");
  if(out[0].abs().compareTo(new BigDecimal(180))>0||out[1].abs().compareTo(new BigDecimal(90))>0)throw new ServiceException("转换后的坐标超出经纬度范围，请核实来源");
  return new BigDecimal[]{out[0].setScale(7,RoundingMode.HALF_UP),out[1].setScale(7,RoundingMode.HALF_UP)};
 }
}
