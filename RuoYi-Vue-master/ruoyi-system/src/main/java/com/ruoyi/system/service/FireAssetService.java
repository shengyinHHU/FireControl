package com.ruoyi.system.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.FireAssetMapper;
import com.ruoyi.system.fire.FireConnections;

@Service
public class FireAssetService {
 @org.springframework.beans.factory.annotation.Autowired private IFirePlaceService places;
 private final FireAssetMapper mapper;private final FireAccessService reads;private final FireConnections connections;
 public FireAssetService(FireAssetMapper m,FireAccessService r,FireConnections c){mapper=m;reads=r;connections=c;}
 private static ServiceException error(String m){return new ServiceException(m);}
 private String text(String v,int max,String label,boolean required){v=v==null?null:v.trim();if(v!=null&&v.isEmpty())v=null;if(required&&v==null)throw error("请填写"+label);if(v!=null&&v.length()>max)throw error(label+"过长");return v;}
 private String address(String v,String label){v=text(v,12,label,false);if(v!=null&&!v.matches("[0-9a-fA-F]{12}"))throw error(label+"必须是报文中的12位十六进制地址");return v==null?null:v.toLowerCase(Locale.ROOT);}
 private String ip(String v,String label){v=text(v,45,label,false);if(v!=null){String[] parts=v.split("\\.",-1);if(parts.length!=4)throw error(label+"需填写IPv4地址");for(String p:parts)if(!p.matches("[0-9]{1,3}")||Integer.parseInt(p)>255)throw error(label+"格式错误");}return v;}
 private Map<String,Object> args(FireAsset a){Map<String,Object> r=new HashMap<>();r.put("a",a);r.put("actor",SecurityUtils.getUsername());r.put("deptId",SecurityUtils.getDeptId());r.put("userId",SecurityUtils.getUserId());return r;}
 public java.util.List<Map<String,Object>> unregistered(){if(!SecurityUtils.isAdmin(SecurityUtils.getUserId()))throw error("未登记报文仅管理员可查看");return mapper.unregistered();}
 public Map<String,Object> gateway(Long id){FireAccessQuery q=new FireAccessQuery();q.setId(String.valueOf(id));var list=reads.gateways(q);if(list.isEmpty())throw error("装置不存在或无权访问");return list.get(0);}
 private Map<String,Object> lock(Long id,Long revision){gateway(id);var row=mapper.lockGateway(id);if(row==null||!"0".equals(row.get("delFlag")))throw error("装置不存在或已删除");checkRevision(row,revision);return row;}
 private void checkRevision(Map<String,Object> row,Long revision){if(revision==null||((Number)row.get("revision")).longValue()!=revision)throw error("记录已被其他人修改，请重新打开后编辑");}
 private void invalidate(String source){if(source==null)return;TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){connections.invalidate(source);}});}
 @Transactional(rollbackFor=Exception.class)
 public Long saveGateway(FireAsset a){
  a.name=text(a.name,255,"装置名称",true);a.unitName=text(a.unitName,255,"所属单位",true);a.deviceCode=text(a.deviceCode,64,"装置编号",false);
  a.manufacturer=text(a.manufacturer,100,"厂家",false);a.model=text(a.model,100,"型号",false);a.remark=text(a.remark,500,"备注",false);
  a.sourceAddress=address(a.sourceAddress,"装置协议地址");a.centerAddress=address(a.centerAddress,"中心协议地址");a.allowedIp=ip(a.allowedIp,"允许来源IP");a.cameraIp=ip(a.cameraIp,"摄像头IP");a.installationStatus=text(a.installationStatus,100,"安装情况",false);
  if(!Set.of("0","1").contains(a.enabled==null?"":a.enabled))throw error("请选择启用或停用");
  if("0".equals(a.enabled)&&(a.sourceAddress==null||a.centerAddress==null||a.allowedIp==null))throw error("启用前需配置协议地址、中心协议地址和允许来源IP");
  var old=a.id==null?null:lock(a.id,a.revision);
  Long oldPlace=old==null||old.get("placeId")==null?null:((Number)old.get("placeId")).longValue();
  if(a.placeId!=null&&!a.placeId.equals(oldPlace)){var q=new FirePlace();q.setPlaceId(a.placeId);if(places==null||places.selectMapPlaces(q).isEmpty())throw error("关联机构不存在或无权访问");}
  var r=args(a);r.put("identityChanged",old!=null&&!Objects.equals(old.get("sourceAddress"),a.sourceAddress));
  try{if(a.id==null){mapper.insertGateway(r);return ((Number)r.get("id")).longValue();}mapper.updateGateway(r);
   if(!Objects.equals(old.get("sourceAddress"),a.sourceAddress)||!Objects.equals(old.get("centerAddress"),a.centerAddress)||!Objects.equals(old.get("allowedIp"),a.allowedIp)||!Objects.equals(old.get("enabled"),a.enabled))invalidate((String)old.get("sourceAddress"));return a.id;}
  catch(org.springframework.dao.DuplicateKeyException e){throw error("装置编号或协议地址已存在（含历史删除记录），请核对");}
 }
 @Transactional(rollbackFor=Exception.class)
 public void deleteGateway(Long id,Long revision){var row=lock(id,revision);Map<String,Object> r=new HashMap<>();r.put("id",id);r.put("actor",SecurityUtils.getUsername());mapper.deleteGateway(r);invalidate((String)row.get("sourceAddress"));}
 @Transactional(rollbackFor=Exception.class)
 public Long saveHost(FireAsset a){
  if(a.gatewayId==null)throw error("请选择所属装置");gateway(a.gatewayId);var g=mapper.lockGateway(a.gatewayId);if(!"0".equals(g.get("delFlag")))throw error("所属装置已删除");
  a.name=text(a.name,255,"主机名称",true);a.manufacturer=text(a.manufacturer,100,"主机品牌",false);a.model=text(a.model,100,"主机型号",false);a.accessMethod=text(a.accessMethod,500,"接入方式",false);
  if((a.systemType==null)!=(a.systemAddress==null))throw error("系统类型和系统地址需同时填写或同时留空");
  if(a.systemType!=null&&(a.systemType<0||a.systemType>255||a.systemAddress<0||a.systemAddress>255))throw error("系统类型和地址范围为0至255");
  var r=args(a);try{if(a.id==null){mapper.insertHost(r);return ((Number)r.get("id")).longValue();}var old=mapper.lockHost(a.id);if(old==null||!"0".equals(old.get("delFlag"))||!a.gatewayId.equals(((Number)old.get("gatewayId")).longValue()))throw error("主机不存在或所属装置不匹配");checkRevision(old,a.revision);mapper.updateHost(r);return a.id;}
  catch(org.springframework.dao.DuplicateKeyException e){throw error("该系统类型和地址已登记（含历史删除记录），请核对");}
 }
 @Transactional(rollbackFor=Exception.class)
 public void deleteHost(Long id,Long gatewayId,Long revision){gateway(gatewayId);mapper.lockGateway(gatewayId);var old=mapper.lockHost(id);if(old==null||!"0".equals(old.get("delFlag"))||!gatewayId.equals(((Number)old.get("gatewayId")).longValue()))throw error("主机不存在或无权访问");checkRevision(old,revision);var r=new HashMap<String,Object>();r.put("id",id);r.put("actor",SecurityUtils.getUsername());mapper.deleteHost(r);}
}
