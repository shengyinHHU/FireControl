package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.FirePlace;

public interface FirePlaceMapper
{
    List<FirePlace> selectMapPlaces(FirePlace query);
}
