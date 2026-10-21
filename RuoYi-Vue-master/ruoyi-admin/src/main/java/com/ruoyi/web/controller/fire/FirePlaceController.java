package com.ruoyi.web.controller.fire;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.ruoyi.common.core.page.TableDataInfo;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.FirePlace;
import com.ruoyi.system.service.IFirePlaceService;

/** 消防场所地图接口，沿用若依登录、权限及数据范围控制。 */
@RestController
@RequestMapping("/fire/places")
public class FirePlaceController extends BaseController
{
    @Autowired
    private IFirePlaceService placeService;

    @PreAuthorize("@ss.hasPermi('fire:places:list')")
    @GetMapping("/map")
    public AjaxResult map(FirePlace query)
    {
        query.setMapOnly(true);
        List<FirePlace> rows = placeService.selectMapPlaces(query);
        // One extra record detects truncation without silently claiming complete coverage.
        boolean truncated = rows.size() > 5000;
        return success(truncated ? rows.subList(0, 5000) : rows).put("truncated", truncated);
    }

    @PreAuthorize("@ss.hasPermi('fire:places:list')")
    @GetMapping("/list")
    public TableDataInfo list(FirePlace query)
    {
        query.setMapOnly(false);
        startPage();
        return getDataTable(placeService.selectMapPlaces(query));
    }

    @PreAuthorize("@ss.hasPermi('fire:places:list')")
    @GetMapping("/{placeId}")
    public AjaxResult getInfo(@PathVariable Long placeId)
    {
        FirePlace query = new FirePlace();
        query.setPlaceId(placeId);
        query.setMapOnly(false);
        List<FirePlace> rows = placeService.selectMapPlaces(query);
        return rows.isEmpty() ? error("场所不存在或无权访问") : success(rows.get(0));
    }
    @PreAuthorize("@ss.hasPermi('fire:places:list')")
    @GetMapping("/{placeId}/contacts")
    public AjaxResult contacts(@PathVariable Long placeId)
    {
        FirePlace query = new FirePlace();
        query.setPlaceId(placeId);
        return success(placeService.selectContacts(query));
    }
}
