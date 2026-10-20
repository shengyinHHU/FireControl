package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.FirePlace;
import com.ruoyi.system.mapper.FirePlaceMapper;
import com.ruoyi.system.service.IFirePlaceService;

@Service
public class FirePlaceServiceImpl implements IFirePlaceService
{
    private static final Set<String> PLACE_TYPES = Set.of("industry", "highrise", "commercial", "elderly");

    @Autowired
    private FirePlaceMapper placeMapper;

    @Override
    @DataScope(deptAlias = "p", userAlias = "p", userField = "supervisor_user_id", permission = "fire:places:list")
    public List<FirePlace> selectMapPlaces(FirePlace query)
    {
        if (StringUtils.isNotEmpty(query.getPlaceType()) && !PLACE_TYPES.contains(query.getPlaceType()))
        {
            throw new ServiceException("不支持的场所类别");
        }
        return placeMapper.selectMapPlaces(query);
    }
}
