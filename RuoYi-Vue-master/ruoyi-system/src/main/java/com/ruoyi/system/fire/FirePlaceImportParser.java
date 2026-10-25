package com.ruoyi.system.fire;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.*;
import com.ruoyi.common.exception.ServiceException;

/** Header-based standard workbook parser. Never evaluates Excel formulas. */
public final class FirePlaceImportParser {
 public static final Map<String,String> LABELS=Map.of("industry","重点企业","highrise","高层建筑","commercial","综合商业体","elderly","养老机构");
 private static final Map<String,String> TYPES=Map.ofEntries(Map.entry("重点企业","industry"),Map.entry("工贸企业","industry"),Map.entry("高层建筑","highrise"),Map.entry("高层住宅","highrise"),Map.entry("综合商业体","commercial"),Map.entry("商业综合体","commercial"),Map.entry("养老机构","elderly"),Map.entry("养老院","elderly"));
 private static final Set<String> FIELDS=Set.of("来源序号","行业类别","对象名称","负责人","联系电话","所属/管理单位","联系人2","联系电话2","联系人3","联系电话3","场所类别","对象类型","详细地址","所属网格","风险等级","对象编码","经度","纬度","坐标系","资料日期","来源文件","备注");
 public static class RowData {
  public int rowNumber; public String sheet,name,type,category,status="new",reason="",sourceKey; public BigDecimal longitude,latitude;
  public Map<String,String> raw=new LinkedHashMap<>();public Map<String,String> cells=new LinkedHashMap<>();public List<String> matches=new ArrayList<>();
  public boolean coordinates(){return longitude!=null&&latitude!=null;}
 }
 public static String hash(byte[] data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(Exception e){throw new IllegalStateException(e);}}
 public static String normalize(String value){return value.toLowerCase(Locale.ROOT).replaceAll("[（(][^（）()]*[）)]","").replaceAll("[\\s\\p{Punct}，。、“”‘’（）·]+","").replaceAll("^(江苏省|江苏|南京市|南京)","").replaceAll("(有限责任公司|股份有限公司|有限公司)$","");}
 public static boolean similar(String left,String right){
  String a=normalize(left),b=normalize(right);if(a.equals(b))return true;
  if(Math.min(a.length(),b.length())<4)return false;
  if(Math.min(a.length(),b.length())>=5&&(a.contains(b)||b.contains(a))&&Math.min(a.length(),b.length())*1.0/Math.max(a.length(),b.length())>=.65)return true;
  if(Math.abs(a.length()-b.length())>Math.max(a.length(),b.length())*.18)return false;
  int[] prev=new int[b.length()+1];for(int j=0;j<prev.length;j++)prev[j]=j;
  for(int i=1;i<=a.length();i++){int[] next=new int[b.length()+1];next[0]=i;for(int j=1;j<=b.length();j++)next[j]=Math.min(Math.min(next[j-1]+1,prev[j]+1),prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));prev=next;}
  return 1-prev[b.length()]*1.0/Math.max(a.length(),b.length())>=.82;
 }
 public static List<RowData> parse(byte[] bytes,String filename)throws IOException {
  if(bytes.length>10*1024*1024)throw new ServiceException("文件不能超过10MB");
  if(filename==null||!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw new ServiceException("请上传单页标准.xlsx文件");
  List<RowData> result=new ArrayList<>();String sha=hash(bytes);
  try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(bytes))){
   if(book.getNumberOfSheets()!=1)throw new ServiceException("请使用只有一个工作表的标准模板");
   Sheet sheet=book.getSheetAt(0);if(sheet.getLastRowNum()>10000)throw new ServiceException("工作表行范围过大，请清除多余空白格式行后再上传");org.apache.poi.ss.usermodel.Row header=sheet.getRow(0);if(header==null)throw new ServiceException("第一行必须为列名");
   if(header.getLastCellNum()>100)throw new ServiceException("表格最多100列，请使用标准模板");
   int totalCharacters=0;Map<String,Integer> columns=new LinkedHashMap<>();DataFormatter format=new DataFormatter(Locale.CHINA);
   for(Cell cell:header){String name=format.formatCellValue(cell).trim();if(FIELDS.contains(name)&&columns.put(name,cell.getColumnIndex())!=null)throw new ServiceException("表头重复："+name);}
   if(!columns.containsKey("对象名称")||(!columns.containsKey("场所类别")&&!columns.containsKey("对象类型")))throw new ServiceException("缺少对象名称或场所类别列");
   if(columns.containsKey("场所类别")&&columns.containsKey("对象类型"))throw new ServiceException("请只保留场所类别列，不要与对象类型重复");
   for(int line=1;line<=sheet.getLastRowNum();line++){
    org.apache.poi.ss.usermodel.Row input=sheet.getRow(line);if(input==null)continue;
    RowData row=new RowData();row.rowNumber=line+1;row.sheet=sheet.getSheetName();List<String> errors=new ArrayList<>();
    for(var column:columns.entrySet()){
     Cell cell=input.getCell(column.getValue());String value=cell==null?"":format.formatCellValue(cell).trim();
     if(cell!=null&&cell.getCellType()==CellType.FORMULA)errors.add(column.getKey()+"不支持公式，请粘贴为值");
     if(value.length()>2000)errors.add(column.getKey()+"内容过长");
     row.cells.put(column.getKey(),value);
    }
    if(row.cells.values().stream().allMatch(String::isBlank))continue;
    for(Cell cell:input){if(cell.getColumnIndex()>=100)throw new ServiceException("表格数据超出100列");String value=format.formatCellValue(cell);totalCharacters+=value.length();if(totalCharacters>5000000)throw new ServiceException("表格文本过多，请分批导入");row.raw.put(cell.getColumnIndex()+":"+format.formatCellValue(header.getCell(cell.getColumnIndex())),value);}
    if(row.cells.getOrDefault("来源序号","").length()>64)errors.add("来源序号超过64字");
    if(result.size()>=2000)throw new ServiceException("一次最多2000条，请分批导入");
    row.name=row.cells.getOrDefault("对象名称","");String label=row.cells.getOrDefault("场所类别",row.cells.getOrDefault("对象类型",""));row.type=TYPES.getOrDefault(label,LABELS.containsKey(label)?label:"");row.category=LABELS.getOrDefault(row.type,label);
    if(row.name.isBlank()||row.name.length()>255)errors.add("对象名称必填且不超过255字");
    if(row.type.isBlank())errors.add("场所类别需为重点企业、高层建筑、综合商业体或养老机构");
    for(var lim:Map.of("行业类别",100,"所属/管理单位",255,"详细地址",500,"风险等级",16,"负责人",100,"联系电话",64,"联系人2",100,"联系电话2",64,"联系人3",100,"联系电话3",64).entrySet())if(row.cells.getOrDefault(lim.getKey(),"").length()>lim.getValue())errors.add(lim.getKey()+"超出长度限制");
    String lng=row.cells.getOrDefault("经度",""),lat=row.cells.getOrDefault("纬度","");
    if(!lng.isBlank()||!lat.isBlank()){
     try{row.longitude=new BigDecimal(lng);row.latitude=new BigDecimal(lat);if(row.longitude.abs().compareTo(new BigDecimal("180"))>0||row.latitude.abs().compareTo(new BigDecimal("90"))>0||row.longitude.signum()==0||row.latitude.signum()==0)throw new IllegalArgumentException();
      if(!"GCJ02".equalsIgnoreCase(row.cells.getOrDefault("坐标系","").replace("-","")))errors.add("有坐标时坐标系必须为GCJ02");
     }catch(Exception e){errors.add("经纬度需成对填写有效数字，不能填0");row.longitude=null;row.latitude=null;}
    }
    row.sourceKey=hash((sha+"|"+row.sheet+"|"+row.rowNumber).getBytes(StandardCharsets.UTF_8));
    if(!errors.isEmpty()){row.status="invalid";row.reason=String.join("；",errors);}
    else if(!row.cells.getOrDefault("对象编码","").isBlank()){row.status="duplicate";row.reason="已填写对象编码，本功能仅新增，跳过以免覆盖已有对象";}
    result.add(row);
   }
  }catch(ServiceException e){throw e;}catch(Exception e){throw new ServiceException("Excel无法读取，请确认文件完整且未加密");}
  if(result.isEmpty())throw new ServiceException("文件中没有可读取的对象数据");return result;
 }
 private FirePlaceImportParser(){}
}
