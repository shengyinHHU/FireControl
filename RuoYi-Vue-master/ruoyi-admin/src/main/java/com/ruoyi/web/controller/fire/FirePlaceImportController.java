package com.ruoyi.web.controller.fire;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.fire.FirePlaceImportService;

@RestController
@RequestMapping("/fire/places/import")
public class FirePlaceImportController extends BaseController {
 private final FirePlaceImportService service;
 public FirePlaceImportController(FirePlaceImportService s){service=s;}
 @PreAuthorize("@ss.hasPermi('fire:places:import') and @ss.hasPermi('fire:places:list')")
 @PostMapping("/preview")
 public AjaxResult preview(@RequestParam("file")MultipartFile file)throws Exception {
  if(file.isEmpty()||file.getSize()>10*1024*1024)throw new ServiceException("请选择不超过10MB的标准.xlsx文件");
  return success(service.preview(file.getBytes(),file.getOriginalFilename(),SecurityUtils.getUserId(),SecurityUtils.isAdmin(SecurityUtils.getUserId())));
 }
 @PreAuthorize("@ss.hasPermi('fire:places:import') and @ss.hasPermi('fire:places:list')")
 @PostMapping("/template")
 public void template(jakarta.servlet.http.HttpServletResponse response)throws Exception {
  response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  response.setHeader("Content-Disposition","attachment; filename=place-import-template.xlsx");
  try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook()){
   var sheet=book.createSheet("监管对象");String[] headers={"来源序号","行业类别","对象名称","负责人","联系电话","所属/管理单位","联系人2","联系电话2","联系人3","联系电话3","场所类别","详细地址","所属网格","风险等级","对象编码","经度","纬度","坐标系","资料日期","来源文件","备注"};
   var header=sheet.createRow(0);for(int i=0;i<headers.length;i++){header.createCell(i).setCellValue(headers[i]);sheet.setColumnWidth(i,22*256);}sheet.createFreezePane(0,1);
   var style=book.createCellStyle();style.setDataFormat(book.createDataFormat().getFormat("@"));for(int col:new int[]{0,4,7,9,14})sheet.setDefaultColumnStyle(col,style);
   var helper=sheet.getDataValidationHelper();var list=helper.createExplicitListConstraint(new String[]{"重点企业","高层建筑","综合商业体","养老机构"});var validation=helper.createValidation(list,new org.apache.poi.ss.util.CellRangeAddressList(1,2000,10,10));validation.setShowErrorBox(true);sheet.addValidationData(validation);book.write(response.getOutputStream());
  }
 }
 public record Confirm(String batchId){}
 @PreAuthorize("@ss.hasPermi('fire:places:import') and @ss.hasPermi('fire:places:list')")
 @Log(title="重点场所导入",businessType=BusinessType.IMPORT,isSaveResponseData=false)
 @PostMapping("/confirm")
 public AjaxResult confirm(@RequestBody Confirm data){return success(service.commit(data.batchId(),SecurityUtils.getUserId(),SecurityUtils.getDeptId(),SecurityUtils.getUsername(),SecurityUtils.isAdmin(SecurityUtils.getUserId())));}
}
