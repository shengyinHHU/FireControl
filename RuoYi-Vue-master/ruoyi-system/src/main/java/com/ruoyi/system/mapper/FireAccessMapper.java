package com.ruoyi.system.mapper;
import java.util.*;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.FireAccessQuery;
public interface FireAccessMapper {
 Map<String,Object> gatewayForUpdate(String source);
 void insertMessage(Map<String,Object> row);
 void updateTransmission(@Param("id") Long id,@Param("status") String status,@Param("error") String error);
 Long sessionGateway(String session);
 void bindRawSession(@Param("session") String session,@Param("gatewayId") Long gatewayId);
 void touchGateway(Map<String,Object> row);
 void updateGatewayState(Map<String,Object> row);
 void upsertHost(Map<String,Object> row);
 Map<String,Object> host(Map<String,Object> row);
 void insertEvent(Map<String,Object> row);
 List<Long> alarmGatewayIds(FireAccessQuery q);
 List<Map<String,Object>> gateways(FireAccessQuery q);
 List<Map<String,Object>> hosts(FireAccessQuery q);
 List<Map<String,Object>> events(FireAccessQuery q);
 List<Map<String,Object>> messages(FireAccessQuery q);
 List<Map<String,Object>> hostConnectionCounts(FireAccessQuery q);
 Long gatewayCount(FireAccessQuery q);
 Map<String,Object> hostStats(FireAccessQuery q);
 Map<String,Object> eventStats(FireAccessQuery q);
}
