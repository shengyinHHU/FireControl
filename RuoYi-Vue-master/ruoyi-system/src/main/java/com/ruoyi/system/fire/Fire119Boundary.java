package com.ruoyi.system.fire;

import java.util.*;
import java.math.BigDecimal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class Fire119Boundary {
 public record Match(String status,String method,String basis,String region) {}
 public static final String VERSION="dc33-0ad25c5e734b";
 private final List<double[]> ring=new ArrayList<>();private final Fire119Properties config;
 public static String scopeVersion(String source){return source+"|"+Fire119Coordinates.VERSION+"|"+VERSION;}
 @SuppressWarnings("unchecked")
 public Fire119Boundary(Fire119Properties p,ObjectMapper json) throws Exception {
  config=p;Map<String,Object> root;
  try(var input=new ClassPathResource("fire/dachang-119-scope.json").getInputStream()){root=json.readValue(input,Map.class);}
  if(!VERSION.equals(root.get("version"))||!"GCJ02".equals(root.get("coordinateSystem")))throw new IllegalArgumentException("119边界版本或坐标系无效");
  for(Object point:(List<?>)root.get("ring")){List<?> xy=(List<?>)point;ring.add(new double[]{((Number)xy.get(0)).doubleValue(),((Number)xy.get(1)).doubleValue()});}
  if(ring.size()!=34||!Arrays.equals(ring.get(0),ring.get(ring.size()-1)))throw new IllegalArgumentException("119边界应为33点闭合多边形");
 }
 public Match match(BigDecimal longitude,BigDecimal latitude){
  String basis="用户33点范围 "+VERSION+"；GCJ-02；转换="+Fire119Coordinates.VERSION;
  if(longitude==null||latitude==null)return new Match("pending","boundary",basis+"；缺少可用高德坐标",null);
  if(!"verified".equals(config.getBoundaryCoordinateMode()))return new Match("pending","boundary",basis+"；边界核验未启用",null);
  boolean inside=covers(ring,longitude.doubleValue(),latitude.doubleValue());
  return new Match(inside?"inside":"outside","boundary",basis+"；地区="+(inside?"大厂街道":"范围外"),inside?"大厂街道":null);
 }
 /** Local metric projection around Dachang; diagnostic proximity only, never expands inclusion. */
 public BigDecimal distance(BigDecimal longitude,BigDecimal latitude){
  if(longitude==null||latitude==null)return null;
  double sx=6371008.8*Math.PI/180*Math.cos(Math.toRadians(32.23)),sy=6371008.8*Math.PI/180,min=Double.POSITIVE_INFINITY;
  for(int i=1;i<ring.size();i++){double[] a=ring.get(i-1),b=ring.get(i);double x=(longitude.doubleValue()-a[0])*sx,y=(latitude.doubleValue()-a[1])*sy,dx=(b[0]-a[0])*sx,dy=(b[1]-a[1])*sy;
   double t=Math.max(0,Math.min(1,(x*dx+y*dy)/(dx*dx+dy*dy)));min=Math.min(min,Math.hypot(x-t*dx,y-t*dy));}
  return BigDecimal.valueOf(min).setScale(3,java.math.RoundingMode.HALF_UP);
 }
 // A point is covered if any original region covers it. Preserve gaps and separate pieces.
 // Boundary points are included; no convex hull, bounding-box approximation or address guessing.
 public static boolean covers(List<double[]> ring,double x,double y){
  boolean inside=false;
  for(int i=0,j=ring.size()-1;i<ring.size();j=i++){
   double[] a=ring.get(j),b=ring.get(i);
   double cross=(x-a[0])*(b[1]-a[1])-(y-a[1])*(b[0]-a[0]);
   if(Math.abs(cross)<1e-12 && x>=Math.min(a[0],b[0])-1e-10 && x<=Math.max(a[0],b[0])+1e-10 && y>=Math.min(a[1],b[1])-1e-10 && y<=Math.max(a[1],b[1])+1e-10)return true;
   if((a[1]>y)!=(b[1]>y) && x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])inside=!inside;
  }
  return inside;
 }
}
