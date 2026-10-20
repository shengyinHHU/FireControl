package com.ruoyi.system.domain;
import com.ruoyi.common.core.domain.BaseEntity;
public class FireAccessQuery extends BaseEntity {
 public java.util.Date getRecentSince(){return new java.util.Date(System.currentTimeMillis()-86400000L);}
 private String id, keyword, kind, gatewayId;
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
 public String getKind(){return kind;} public void setKind(String v){kind=v;}
 public String getGatewayId(){return gatewayId;} public void setGatewayId(String v){gatewayId=v;}
}
