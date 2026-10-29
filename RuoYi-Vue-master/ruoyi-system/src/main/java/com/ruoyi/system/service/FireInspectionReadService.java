package com.ruoyi.system.service;
import java.util.*;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.FireInspectionMapper;
import com.ruoyi.system.domain.FireInspectionQuery;
@Service
public class FireInspectionReadService {
 private final FireInspectionMapper mapper;
 public FireInspectionReadService(FireInspectionMapper m){mapper=m;}
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:inspections:list")
 public List<Map<String,Object>> posts(FireInspectionQuery q){return mapper.posts(q);}
 @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:inspections:list")
 public List<Map<String,Object>> records(FireInspectionQuery q){return mapper.records(q);}
}
