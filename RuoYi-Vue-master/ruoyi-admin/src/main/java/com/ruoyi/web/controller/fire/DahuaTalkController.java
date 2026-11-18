package com.ruoyi.web.controller.fire;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.framework.websocket.DahuaTalkService;
import com.ruoyi.system.fire.dahua.*;

@RestController
@RequestMapping("/fire/video/dahua")
public class DahuaTalkController extends BaseController {
 private final DahuaEndpointStore store;private final DahuaTalkService calls;private final DahuaProperties config;
 public DahuaTalkController(DahuaEndpointStore s,DahuaTalkService c,DahuaProperties p){store=s;calls=c;config=p;}
 @GetMapping("/devices/{code}/binding") @PreAuthorize("@ss.hasPermi('fire:video:talk-config')")
 public AjaxResult binding(@PathVariable String code){var e=store.get(store.camera(code));Map<String,Object> result=new HashMap<>();result.put("endpoint",e);result.put("profiles",config.getCredentials().keySet());return success(result);}
 @PutMapping("/devices/{code}/binding") @PreAuthorize("@ss.hasPermi('fire:video:talk-config')")
 public AjaxResult save(@PathVariable String code,@RequestBody DahuaEndpointStore.Edit edit){return success(calls.save(store.camera(code),edit,getUsername()));}
 @DeleteMapping("/devices/{code}/binding") @PreAuthorize("@ss.hasPermi('fire:video:talk-config')")
 public AjaxResult delete(@PathVariable String code,@RequestParam int revision){calls.delete(store.camera(code),revision);return success();}
 @PostMapping("/devices/{code}/sessions") @PreAuthorize("@ss.hasPermi('fire:video:talk')")
 public ResponseEntity<AjaxResult> ticket(@PathVariable String code){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(success(calls.ticket(store.camera(code),getUserId())));}
 @DeleteMapping("/sessions") @PreAuthorize("@ss.hasPermi('fire:video:talk')")
 public AjaxResult stop(@RequestBody Map<String,String> body){calls.cancel(body.get("ticket"),getUserId());return success();}
}
