package com.ruoyi.web.controller.fire;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.FireAlarmSimulationService;

@RestController
@RequestMapping("/fire/alarms/simulations")
public class FireAlarmSimulationController extends BaseController {
 private final FireAlarmSimulationService service;
 public FireAlarmSimulationController(FireAlarmSimulationService s){service=s;}
 @PreAuthorize("@ss.hasPermi('fire:alarms:list') and @ss.hasPermi('fire:alarms:simulate') and @ss.hasPermi('fire:places:list')")
 @PostMapping @Log(title="模拟报警",businessType=BusinessType.INSERT)
 public AjaxResult create(@RequestBody FireAlarmSimulationService.Create body){return success(service.create(body));}
 @PreAuthorize("@ss.hasPermi('fire:alarms:list')")
 @PostMapping("/{id}/handle") @Log(title="模拟警情处理",businessType=BusinessType.UPDATE)
 public AjaxResult handle(@PathVariable String id,@RequestBody FireAlarmSimulationService.Handle body){service.handle(id,body);return success();}
}
