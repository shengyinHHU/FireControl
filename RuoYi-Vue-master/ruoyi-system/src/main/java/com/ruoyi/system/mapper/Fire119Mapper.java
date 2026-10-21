package com.ruoyi.system.mapper;
import java.util.*;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.FireAccessQuery;
public interface Fire119Mapper {
 Map<String,Object> incidentForUpdate(@Param("source") String source,@Param("externalId") String externalId);
 void insertIncident(Map<String,Object> row);
 void updateIncident(Map<String,Object> row);
 void touchIncident(Map<String,Object> row);
 void insertHistory(Map<String,Object> row);
 void writeState(Map<String,Object> row);
 void insertLog(Map<String,Object> row);
 List<Map<String,Object>> alarms(FireAccessQuery query);
 Map<String,Object> scopeCounts(FireAccessQuery query);
 Map<String,Object> syncState(@Param("source") String source,@Param("region") String region);
}
