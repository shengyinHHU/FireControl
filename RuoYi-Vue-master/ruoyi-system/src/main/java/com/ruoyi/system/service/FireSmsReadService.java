package com.ruoyi.system.service;
import java.util.*;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.system.domain.FireSmsQuery;
import com.ruoyi.system.mapper.FireSmsMapper;
@Service
public class FireSmsReadService {
    private final FireSmsMapper mapper;
    public FireSmsReadService(FireSmsMapper mapper) { this.mapper = mapper; }
    @DataScope(deptAlias="s", userAlias="s", userField="sender_user_id", permission="fire:sms:list")
    public List<Map<String,Object>> records(FireSmsQuery query) { return mapper.records(query); }
}
