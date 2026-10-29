package com.ruoyi.web.controller.fire;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FireInspectionQuery;
import com.ruoyi.system.service.FireInspectionReadService;
import com.ruoyi.system.fire.FireInspectionService;
@RestController
@RequestMapping("/fire/inspection")
public class FireInspectionController extends BaseController {
 private final FireInspectionReadService reads;private final FireInspectionService service;private final tools.jackson.databind.ObjectMapper json;
 public FireInspectionController(FireInspectionReadService r,FireInspectionService s,tools.jackson.databind.ObjectMapper j){reads=r;service=s;json=j;}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list')") @GetMapping("/posts")
 public TableDataInfo posts(FireInspectionQuery q){startPage();return getDataTable(reads.posts(q));}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list')") @GetMapping("/posts/{id}")
 public AjaxResult post(@PathVariable long id){return success(service.post(id));}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list') and @ss.hasPermi('fire:inspections:edit')") @PostMapping("/posts")
 public AjaxResult create(@RequestBody Map<String,Object> input)throws Exception{return success(service.savePost(null,input));}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list') and @ss.hasPermi('fire:inspections:edit')") @PutMapping("/posts/{id}")
 public AjaxResult update(@PathVariable long id,@RequestBody Map<String,Object> input)throws Exception{return success(service.savePost(id,input));}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list') and @ss.hasPermi('fire:video:preview')") @PostMapping("/posts/{id}/cameras/{binding}/preview")
 public AjaxResult preview(@PathVariable long id,@PathVariable long binding,@RequestParam(defaultValue="flv") String protocol)throws Exception{return success(service.preview(id,binding,protocol));}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list')") @GetMapping("/records")
 public TableDataInfo records(FireInspectionQuery q){startPage();return getDataTable(reads.records(q));}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list')") @GetMapping("/records/{id}")
 public AjaxResult record(@PathVariable long id){return success(service.record(id));}
 @PreAuthorize("@ss.hasPermi('fire:inspections:list') and @ss.hasPermi('fire:inspections:add')") @PostMapping("/records")
 public AjaxResult createRecord(@RequestParam("data") String data,@RequestParam(required=false) MultipartFile evidence)throws Exception {
  if(evidence!=null&&evidence.getSize()>5*1024*1024)throw new ServiceException("抓拍图片不能超过5MB");
  return success(service.saveRecord(json.readValue(data,Map.class),evidence==null?null:evidence.getBytes()));
 }
 @PreAuthorize("@ss.hasPermi('fire:inspections:list')") @GetMapping("/records/{id}/evidence")
 public ResponseEntity<byte[]> evidence(@PathVariable long id)throws Exception {return ResponseEntity.ok().header("Content-Type","image/jpeg").header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(service.evidence(id));}
}
