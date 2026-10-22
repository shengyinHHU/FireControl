package com.ruoyi.web.controller.fire;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.fire.FireVideoClient;
@RestController
@RequestMapping("/fire/video")
public class FireVideoController extends BaseController {
 private final FireVideoClient client; private final com.ruoyi.system.fire.FireVideoMedia media;
 public FireVideoController(FireVideoClient c,com.ruoyi.system.fire.FireVideoMedia m){client=c;media=m;}
 @PreAuthorize("@ss.hasPermi('fire:video:list')") @GetMapping("/regions")
 public AjaxResult regions(@RequestParam(required=false) Long regionId) throws Exception{return success(client.regions(regionId));}
 @PreAuthorize("@ss.hasPermi('fire:video:list')") @GetMapping("/devices")
 public AjaxResult devices(@RequestParam(required=false) Long regionId,@RequestParam(defaultValue="-1") long lastId,@RequestParam(defaultValue="20") int pageSize) throws Exception{return success(client.devices(regionId,lastId,pageSize));}
 @PreAuthorize("@ss.hasPermi('fire:video:preview')") @PostMapping("/devices/{code}/preview")
 public AjaxResult preview(@PathVariable String code,@RequestParam(defaultValue="hls") String protocol) throws Exception{return success(client.preview(code,protocol));}
 @PreAuthorize("@ss.hasPermi('fire:video:preview')") @GetMapping("/media/{session}/{resource}")
 public org.springframework.http.ResponseEntity<byte[]> media(@PathVariable String session,@PathVariable int resource) throws Exception {
  var payload=media.read(session,resource);
  return org.springframework.http.ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Type",payload.contentType()).body(payload.body());
 }
}

