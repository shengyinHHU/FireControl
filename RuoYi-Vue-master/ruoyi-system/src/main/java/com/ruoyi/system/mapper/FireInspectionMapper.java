package com.ruoyi.system.mapper;
import java.util.*;
import com.ruoyi.system.domain.FireInspectionQuery;
public interface FireInspectionMapper {
 List<Map<String,Object>> posts(FireInspectionQuery query);
 List<Map<String,Object>> records(FireInspectionQuery query);
}
