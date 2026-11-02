package com.ruoyi.web.controller.fire;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.fire.*;

@RestController
@RequestMapping("/fire/video/catalog")
public class FireVideoCatalogController extends BaseController {
 private final FireVideoCatalogService service;
 public FireVideoCatalogController(FireVideoCatalogService service){this.service=service;}
 @PreAuthorize("@ss.hasPermi('fire:video:list')") @GetMapping
 public AjaxResult list(@RequestParam(required=false)String keyword,@RequestParam(required=false)String directory,@RequestParam(required=false)String location,@RequestParam(defaultValue="1")int pageNum,@RequestParam(defaultValue="20")int pageSize){return success(service.list(keyword,directory,location,pageNum,pageSize));}
 @PreAuthorize("@ss.hasPermi('fire:video:list')") @GetMapping("/directories")
 public AjaxResult directories(){return success(service.directories());}
 @PreAuthorize("@ss.hasPermi('fire:video:list')") @GetMapping("/map")
 public AjaxResult map(){return success(service.map());}
 @PreAuthorize("@ss.hasPermi('fire:video:list')") @GetMapping("/{id}")
 public AjaxResult get(@PathVariable long id){return success(service.get(id));}
 @PreAuthorize("@ss.hasPermi('fire:video:list') and @ss.hasPermi('fire:video:sync')")
 @Log(title="同步监控目录",businessType=BusinessType.UPDATE) @PostMapping("/sync")
 public AjaxResult sync(){return success(service.startSync());}
 @PreAuthorize("@ss.hasPermi('fire:video:list') and @ss.hasPermi('fire:video:export')")
 @Log(title="监控坐标导出",businessType=BusinessType.EXPORT,isSaveResponseData=false) @PostMapping("/export")
 public void export(@RequestParam(required=false)String keyword,@RequestParam(required=false)String directory,@RequestParam(required=false)String location,jakarta.servlet.http.HttpServletResponse response)throws Exception{
  var rows=service.export(keyword,directory,location);
  response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Content-Disposition","attachment; filename=video-coordinates.xlsx");response.setHeader("Cache-Control","no-store");FireVideoCoordinateExcel.write(rows,response.getOutputStream());
 }
 @PreAuthorize("@ss.hasPermi('fire:video:list') and @ss.hasPermi('fire:video:import')") @PostMapping("/import/preview")
 public AjaxResult preview(@RequestParam("file")MultipartFile file,@RequestParam(defaultValue="fill")String mode)throws Exception{
  if(file.isEmpty()||file.getSize()>10*1024*1024)throw new ServiceException("请选择不超过10MB的.xlsx文件");return success(service.preview(file.getBytes(),file.getOriginalFilename(),mode,SecurityUtils.getUserId()));
 }
 public record Confirm(String batchId){}
 @PreAuthorize("@ss.hasPermi('fire:video:list') and @ss.hasPermi('fire:video:import')")
 @Log(title="监控坐标导入",businessType=BusinessType.IMPORT,isSaveRequestData=false,isSaveResponseData=false) @PostMapping("/import/confirm")
 public AjaxResult confirm(@RequestBody Confirm data){return success(service.commit(data.batchId(),SecurityUtils.getUserId(),SecurityUtils.getUsername()));}
 @PreAuthorize("@ss.hasPermi('fire:video:list') and @ss.hasPermi('fire:video:edit')")
 @Log(title="监控坐标维护",businessType=BusinessType.UPDATE,isSaveRequestData=false,isSaveResponseData=false) @PutMapping("/{id}/coordinates")
 public AjaxResult edit(@PathVariable long id,@RequestBody FireVideoCoordinateExcel.CoordinateRow data){return success(service.edit(id,data,SecurityUtils.getUserId(),SecurityUtils.getUsername()));}
}
