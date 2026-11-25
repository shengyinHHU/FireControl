package com.ruoyi.system.mapper;
import java.util.*;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.FireAccessQuery;
public interface FireAccessMapper {
 Map<String,Object> gatewayForUpdate(String ip);
 void bindObservedGateway(@Param("source") String source,@Param("center") String center,@Param("ip") String ip);
 void registerTrustedGateway(@Param("source") String source,@Param("center") String center,@Param("ip") String ip);
 void insertMessage(Map<String,Object> row);
 void updateTransmission(@Param("id") Long id,@Param("status") String status,@Param("error") String error);
 Long sessionGateway(String session);
 Long messageGateway(Long id);
 List<Map<String,Object>> gatewayPresence();
 void bindRawSession(@Param("session") String session,@Param("gatewayId") Long gatewayId);
 void touchGateway(Map<String,Object> row);
 void updateGatewayState(Map<String,Object> row);
 void insertEvent(Map<String,Object> row);
 List<Long> alarmGatewayIds(FireAccessQuery q);
 List<Map<String,Object>> gateways(FireAccessQuery q);
 List<Map<String,Object>> events(FireAccessQuery q);
 List<Map<String,Object>> messages(FireAccessQuery q);
 Long gatewayCount(FireAccessQuery q);
 Map<String,Object> eventStats(FireAccessQuery q);
}
