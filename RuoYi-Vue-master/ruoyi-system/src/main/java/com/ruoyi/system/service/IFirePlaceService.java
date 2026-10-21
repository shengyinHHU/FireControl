package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.FirePlace;

public interface IFirePlaceService
{
    List<FirePlace> selectMapPlaces(FirePlace query);
    List<java.util.Map<String, Object>> selectContacts(FirePlace query);
}
