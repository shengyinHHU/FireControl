package com.ruoyi.web.controller.fire;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.FireAsset;
import com.ruoyi.system.service.FireAssetService;
@RestController
@RequestMapping("/fire/assets")
public class FireAssetController extends BaseController {
 private final FireAssetService service;
 public FireAssetController(FireAssetService s){service=s;}
 @GetMapping("/unregistered") @PreAuthorize("@ss.hasPermi('fire:hosts:list')")
 public AjaxResult unregistered(){return success(service.unregistered());}
 @GetMapping("/gateways/{id}") @PreAuthorize("@ss.hasPermi('fire:hosts:list')")
 public AjaxResult gateway(@PathVariable Long id){return success(service.gateway(id));}
 @PostMapping("/gateways") @PreAuthorize("@ss.hasPermi('fire:hosts:add')") @Log(title="传输装置",businessType=BusinessType.INSERT)
 public AjaxResult addGateway(@RequestBody FireAsset a){a.id=null;return success(service.saveGateway(a));}
 @PutMapping("/gateways/{id}") @PreAuthorize("@ss.hasPermi('fire:hosts:edit')") @Log(title="传输装置",businessType=BusinessType.UPDATE)
 public AjaxResult editGateway(@PathVariable Long id,@RequestBody FireAsset a){a.id=id;return success(service.saveGateway(a));}
 @DeleteMapping("/gateways/{id}") @PreAuthorize("@ss.hasPermi('fire:hosts:remove')") @Log(title="传输装置",businessType=BusinessType.DELETE)
 public AjaxResult deleteGateway(@PathVariable Long id,@RequestParam Long revision){service.deleteGateway(id,revision);return success();}
 @PostMapping("/hosts") @PreAuthorize("@ss.hasPermi('fire:hosts:add')") @Log(title="下挂消防主机",businessType=BusinessType.INSERT)
 public AjaxResult addHost(@RequestBody FireAsset a){a.id=null;return success(service.saveHost(a));}
 @PutMapping("/hosts/{id}") @PreAuthorize("@ss.hasPermi('fire:hosts:edit')") @Log(title="下挂消防主机",businessType=BusinessType.UPDATE)
 public AjaxResult editHost(@PathVariable Long id,@RequestBody FireAsset a){a.id=id;return success(service.saveHost(a));}
 @DeleteMapping("/hosts/{id}") @PreAuthorize("@ss.hasPermi('fire:hosts:remove')") @Log(title="下挂消防主机",businessType=BusinessType.DELETE)
 public AjaxResult deleteHost(@PathVariable Long id,@RequestParam Long gatewayId,@RequestParam Long revision){service.deleteHost(id,gatewayId,revision);return success();}
}
