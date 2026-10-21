package com.ruoyi.system.fire;

import java.util.*;
import java.math.BigDecimal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class Fire119Boundary {
 public record Match(String status,String method,String basis,String region) {}
 private record Region(String name,List<double[]> ring) {}
 private final List<Region> regions=new ArrayList<>();private final Fire119Properties config;private final String version;
 @SuppressWarnings("unchecked")
 public Fire119Boundary(Fire119Properties p,ObjectMapper json) throws Exception {
  config=p;Map<String,Object> root;
  try(var input=new ClassPathResource("fire/dachang-119-boundary.json").getInputStream()){root=json.readValue(input,Map.class);}
  version=String.valueOf(root.get("documentSha256"));
  for(Object obj:(List<?>)root.get("features")){
   var feature=(Map<String,Object>)obj;var props=(Map<String,Object>)feature.get("properties");var geometry=(Map<String,Object>)feature.get("geometry");
   List<double[]> ring=new ArrayList<>();
   for(Object point:(List<?>)((List<?>)geometry.get("coordinates")).get(0)){List<?> xy=(List<?>)point;ring.add(new double[]{((Number)xy.get(0)).doubleValue(),((Number)xy.get(1)).doubleValue()});}
   if(ring.size()<4)throw new IllegalArgumentException("街道边界点不足");regions.add(new Region(String.valueOf(props.get("name")),ring));
  }
  if(regions.size()!=25)throw new IllegalArgumentException("街道资料应包含25个区域");
 }
 public Match match(BigDecimal longitude,BigDecimal latitude){
  String mode=config.getBoundaryCoordinateMode();
  if(longitude==null||latitude==null)return new Match("pending","boundary","大厂街道.doc；缺少有效坐标",null);
  if(!Set.of("verified","raw-unverified").contains(mode))return new Match("pending","boundary","大厂街道.doc；源坐标与边界坐标系兼容性待确认",null);
  List<String> names=new ArrayList<>();
  for(Region region:regions)if(covers(region.ring,longitude.doubleValue(),latitude.doubleValue()))names.add(region.name);
  String basis="大厂街道.doc sha256="+version+"；"+(mode.equals("verified")?"坐标兼容已确认":"按原坐标比较，坐标系待核验")+"；地区="+String.join("、",names);
  return new Match(names.isEmpty()?"outside":"inside","boundary",basis,names.isEmpty()?null:String.join("、",names));
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
