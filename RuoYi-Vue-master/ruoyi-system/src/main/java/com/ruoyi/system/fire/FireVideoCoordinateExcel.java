package com.ruoyi.system.fire;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ruoyi.common.exception.ServiceException;

public final class FireVideoCoordinateExcel {
 public static final int MAX_ROWS=50000;
 public static final String[] HEADERS={"视频来源","设备编码","监控名称","完整目录","安装位置","经度","纬度","坐标系","调查备注","坐标版本"};
 private FireVideoCoordinateExcel(){}
 public static class CoordinateRow {
  public int rowNumber,revision=-1;public long cameraId;public boolean manualVerification=false;
  public String source="",deviceCode="",name="",installationLocation="",remark="",crs="",status="ready",reason="",locationStatus="review";
  public BigDecimal sourceLongitude,sourceLatitude,longitude,latitude,oldLongitude,oldLatitude;
 }
 public static List<CoordinateRow> read(byte[] bytes,String filename)throws IOException{
  if(filename==null||filename.length()>255||!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")||bytes.length>10*1024*1024)throw new ServiceException("请选择不超过10MB的.xlsx文件");
  List<CoordinateRow> rows=new ArrayList<>();var formatter=new DataFormatter(Locale.ROOT);
  try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes))){
   var sheet=book.getSheet("监控坐标");if(sheet==null)throw new ServiceException("缺少“监控坐标”工作表，请使用系统导出的Excel");
   if(sheet.getLastRowNum()>MAX_ROWS)throw new ServiceException("单次最多导入50000条监控");
   var header=sheet.getRow(0);Map<String,Integer> columns=new HashMap<>();
   if(header!=null)for(Cell c:header){String s=formatter.formatCellValue(c).trim();if(columns.put(s,c.getColumnIndex())!=null&&!s.isEmpty())throw new ServiceException("Excel包含重复表头："+s);}
   for(String h:HEADERS)if(!columns.containsKey(h))throw new ServiceException("Excel缺少表头："+h);
   for(int i=1;i<=sheet.getLastRowNum();i++){
    Row excel=sheet.getRow(i);if(excel==null)continue;String[] values=new String[HEADERS.length];boolean empty=true,formula=false,tooLong=false;
    for(int j=0;j<values.length;j++){Cell cell=excel.getCell(columns.get(HEADERS[j]));values[j]=cell==null?"":formatter.formatCellValue(cell).trim();empty&=values[j].isEmpty();formula|=cell!=null&&cell.getCellType()==CellType.FORMULA;tooLong|=values[j].length()>2000;}
    if(empty)continue;CoordinateRow r=new CoordinateRow();r.rowNumber=i+1;r.source=values[0];r.deviceCode=values[1];r.name=values[2];r.installationLocation=values[4];r.crs=FireVideoCoordinates.system(values[7]);r.remark=values[8];rows.add(r);
    try{
     if(formula)throw new ServiceException("不接受公式单元格，请粘贴为数值或文本");if(tooLong||r.installationLocation.length()>500||r.remark.length()>1000)throw new ServiceException("安装位置最多500字，备注最多1000字");
     if(values[5].isEmpty()&&values[6].isEmpty()){r.status="skipped";r.reason="经纬度均为空，保留原坐标";continue;}
     Cell code=excel.getCell(columns.get("设备编码"));if(code==null||code.getCellType()!=CellType.STRING)throw new ServiceException("设备编码必须为文本，请重新导出后填写");
     if(!r.deviceCode.matches("[A-Za-z0-9_.:-]{1,128}"))throw new ServiceException("设备编码格式无效");
     r.revision=Integer.parseInt(values[9]);if(r.revision<0)throw new ServiceException("坐标版本无效，请重新导出");
     r.sourceLongitude=FireVideoCoordinates.number(values[5]);r.sourceLatitude=FireVideoCoordinates.number(values[6]);var mapped=FireVideoCoordinates.convert(r.sourceLongitude,r.sourceLatitude,r.crs);r.longitude=mapped[0];r.latitude=mapped[1];
    }catch(Exception e){r.status="invalid";r.reason=e instanceof ServiceException?e.getMessage():"坐标版本无效，请重新导出";}
   }
  }
  Map<String,Integer> counts=new HashMap<>();for(var row:rows)if(!row.deviceCode.isEmpty())counts.merge(row.source+"\n"+row.deviceCode,1,Integer::sum);
  for(var row:rows)if(counts.getOrDefault(row.source+"\n"+row.deviceCode,0)>1){row.status="invalid";row.reason="文件内设备编码重复，请保留一行";}
  if(rows.isEmpty())throw new ServiceException("工作表没有可检查的监控记录");return rows;
 }
 public static void write(List<Map<String,Object>> rows,OutputStream output)throws IOException{
  try(var book=new XSSFWorkbook()){
   var sheet=book.createSheet("监控坐标");var header=sheet.createRow(0);var headerStyle=book.createCellStyle();var font=book.createFont();font.setBold(true);headerStyle.setFont(font);headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
   var text=book.createCellStyle();text.setDataFormat(book.createDataFormat().getFormat("@"));
   var editable=book.createCellStyle();editable.setLocked(false);editable.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());editable.setFillPattern(FillPatternType.SOLID_FOREGROUND);
   var numeric=book.createCellStyle();numeric.cloneStyleFrom(editable);numeric.setDataFormat(book.createDataFormat().getFormat("0.0000000"));
   for(int i=0;i<HEADERS.length;i++){var c=header.createCell(i);c.setCellValue(HEADERS[i]);c.setCellStyle(headerStyle);sheet.setColumnWidth(i,(i==3?50:i==1?30:22)*256);}
   for(var data:rows){var row=sheet.createRow(sheet.getLastRowNum()+1);Object[] values={data.get("source"),data.get("deviceCode"),data.get("name"),data.get("directoryPath"),data.get("installationLocation"),data.get("lng"),data.get("lat"),data.get("lng")==null?"":"GCJ-02",data.get("remark"),data.get("revision")};
    for(int col=0;col<values.length;col++){var cell=row.createCell(col);if(col==5||col==6){cell.setCellStyle(numeric);if(values[col]!=null)cell.setCellValue(((Number)values[col]).doubleValue());}else if(col==9)cell.setCellValue(((Number)values[col]).doubleValue());else{cell.setCellStyle(col>=4&&col<=8?editable:text);cell.setCellValue(values[col]==null?"":String.valueOf(values[col]));}}
   }
   sheet.createFreezePane(2,1);sheet.setAutoFilter(new CellRangeAddress(0,Math.max(1,sheet.getLastRowNum()),0,HEADERS.length-1));
   var helper=sheet.getDataValidationHelper();var validation=helper.createValidation(helper.createExplicitListConstraint(new String[]{"GCJ-02","WGS84","BD-09"}),new CellRangeAddressList(1,Math.max(1,sheet.getLastRowNum()),7,7));validation.setShowErrorBox(true);sheet.addValidationData(validation);sheet.protectSheet("coordinate-template");
   var guide=book.createSheet("填写说明");String[] notes={"黄色列可填写；设备编码及坐标版本不可修改。保护仅用于防误操作，服务器会重新验证身份与权限。","未定位设备的经纬度为空；两项都为空时跳过，不清除已有坐标。经度在前，纬度在后。","请填写采集工具的真实坐标系：高德GCJ-02、GPS WGS84、百度BD-09。未知坐标系请先核实。","导入默认仅补空坐标；覆盖已有坐标须在页面选择覆盖模式并核对前后差异。","名称、目录只供辨认，按视频来源和设备编码匹配。导入不新增设备、不修改平台名称。","系统将WGS84/BD-09转换为GCJ-02，保留原始值；导入位置标记待核验。","最多50000行、10MB，仅支持.xlsx；不接受公式。可调整数据行顺序，请保留监控坐标表头。"};for(int i=0;i<notes.length;i++)guide.createRow(i).createCell(0).setCellValue(notes[i]);guide.setColumnWidth(0,120*256);book.write(output);
  }
 }
}
