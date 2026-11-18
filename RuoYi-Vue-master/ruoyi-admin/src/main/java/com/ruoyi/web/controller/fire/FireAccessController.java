package com.ruoyi.web.controller.fire;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.FireAccessQuery;
import com.ruoyi.system.service.FireAccessService;

@RestController
@RequestMapping("/fire")
public class FireAccessController extends BaseController {
 private final FireAccessService service;
 private final com.ruoyi.system.service.FireAlarmQueryService alarms;
 private void paged(){startPage();var page=com.github.pagehelper.PageHelper.getLocalPage();if(page!=null)page.setPageSize(Math.max(1,Math.min(page.getPageSize(),1000)));}

 public FireAccessController(FireAccessService s,com.ruoyi.system.service.FireAlarmQueryService a){service=s;alarms=a;}
 @PreAuthorize("@ss.hasPermi('fire:gateways:list')") @GetMapping("/gateways/list")
 public TableDataInfo gateways(FireAccessQuery q){paged();return getDataTable(service.gateways(q));}
 @PreAuthorize("@ss.hasPermi('fire:alarms:list')") @GetMapping("/alarms/list")
 public TableDataInfo events(FireAccessQuery q){paged();return getDataTable(alarms.alarms(q));}
 @PreAuthorize("@ss.hasPermi('fire:alarms:list')") @GetMapping("/alarms/{id}")
 public AjaxResult event(@PathVariable String id){FireAccessQuery q=new FireAccessQuery();q.setId(id);var rows=alarms.alarms(q);return rows.isEmpty()?error("事件不存在或无权访问"):success(rows.get(0));}
 @PreAuthorize("@ss.hasPermi('fire:alarms:list')") @GetMapping("/119/status")
 public AjaxResult sync119Status(){return success(alarms.status(new FireAccessQuery()));}
 @PreAuthorize("@ss.hasPermi('fire:gateways:list')") @GetMapping("/protocol/messages")
 public TableDataInfo messages(FireAccessQuery q){paged();return getDataTable(service.messages(q));}
 @PreAuthorize("@ss.hasPermi('fire:gateways:list')") @GetMapping("/gateways/stats")
 public AjaxResult gatewayStats(){return success(service.gatewayStats(new FireAccessQuery()));}
 @PreAuthorize("@ss.hasPermi('fire:alarms:list')") @GetMapping("/alarms/stats")
 public AjaxResult eventStats(){return success(service.eventStats(new FireAccessQuery()));}
}
