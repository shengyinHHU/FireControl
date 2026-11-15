package com.ruoyi.system.mapper;
import java.util.Map;
public interface FireAssetMapper {
 java.util.List<Map<String,Object>> unregistered();
 Map<String,Object> lockGateway(Long id);
 void insertGateway(Map<String,Object> row);
 void updateGateway(Map<String,Object> row);
 void deleteGateway(Map<String,Object> row);
}
