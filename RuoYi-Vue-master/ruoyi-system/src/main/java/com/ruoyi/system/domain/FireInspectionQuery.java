package com.ruoyi.system.domain;
import com.ruoyi.common.core.domain.BaseEntity;
public class FireInspectionQuery extends BaseEntity {
 private Long id,postId;private String keyword,status,result,beginTime,endTime;
 public Long getId(){return id;}public void setId(Long v){id=v;}
 public Long getPostId(){return postId;}public void setPostId(Long v){postId=v;}
 public String getKeyword(){return keyword;}public void setKeyword(String v){keyword=v;}
 public String getStatus(){return status;}public void setStatus(String v){status=v;}
 public String getResult(){return result;}public void setResult(String v){result=v;}
 public String getBeginTime(){return beginTime;}public void setBeginTime(String v){beginTime=v;}
 public String getEndTime(){return endTime;}public void setEndTime(String v){endTime=v;}
}
