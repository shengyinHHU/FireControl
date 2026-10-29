package com.ruoyi.system.mapper;
import java.util.*;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.FireAccessQuery;
public interface Fire119Mapper {
 List<Map<String,Object>> coordinateCandidates(@Param("source") String source,@Param("version") String version,@Param("limit") int limit);
 int rebuildCoordinates(Map<String,Object> row);
 Map<String,Object> incidentForUpdate(@Param("source") String source,@Param("externalId") String externalId);
 List<Map<String,Object>> incidentHashes(@Param("source") String source,@Param("ids") List<String> ids);
 int touchIncidents(@Param("source") String source,@Param("rows") List<Map<String,Object>> rows);
 void writeQuickState(Map<String,Object> row);
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
