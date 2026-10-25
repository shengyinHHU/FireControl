import fs from 'node:fs/promises';
import {Workbook,SpreadsheetFile} from '@oai/artifact-tool';
const wb=Workbook.create();
const obj=wb.worksheets.add('监管对象'), con=wb.worksheets.add('联系人'), guide=wb.worksheets.add('填写说明');
const headers=['对象编码','临时关联编号','对象名称','对象类型','行业类别','所属/管理单位','详细地址','所属网格','风险等级','经度','纬度','坐标系','来源单位','资料日期','备注'];
const ch=['对象编码','临时关联编号','联系人姓名','职务/联系角色','联系电话','是否主要联系人','备注'];
function style(s,last,heads){s.showGridLines=false;s.tabColor='#157A81';s.getRange(`A1:${last}501`).format.font.name='Microsoft YaHei';s.getRange(`A1:${last}501`).format.font.size=11;s.getRange(`A1:${last}501`).format.rowHeight=25;s.getRange(`A1:${last}501`).format.columnWidth=20;s.getRange(`A1:${last}1`).values=[heads];s.getRange(`A1:${last}1`).format.fill='#164B59';s.getRange(`A1:${last}1`).format.font.color='#FFFFFF';s.getRange(`A1:${last}1`).format.font.bold=true;s.getRange(`A1:${last}1`).format.rowHeight=34;s.freezePanes.freezeRows(1);s.getRange(`A2:${last}501`).setNumberFormat('@');for(let i=2;i<=31;i+=2)s.getRange(`A${i}:${last}${i}`).format.fill='#F0F6F7';}
style(obj,'O',headers);style(con,'G',ch);
obj.getRange('C:C').format.columnWidth=32;obj.getRange('F:G').format.columnWidth=38;obj.getRange('O:O').format.columnWidth=35;con.getRange('D:D').format.columnWidth=27;con.getRange('E:E').format.columnWidth=25;
obj.getRange('D2:D501').dataValidation={rule:{type:'list',values:['高层住宅','工贸企业','商业综合体','养老机构','住宅小区','其他企业','其他场所','待确认']}};
obj.getRange('I2:I501').dataValidation={rule:{type:'list',values:['待确认']}};
obj.getRange('L2:L501').dataValidation={rule:{type:'list',values:['WGS84','GCJ02','BD09']}};
con.getRange('F2:F501').dataValidation={rule:{type:'list',values:['是','否']}};
obj.getRange('J2:K501').setNumberFormat('0.000000');obj.getRange('N2:N501').setNumberFormat('yyyy-mm-dd');
const rows=[
['监管对象标准导入模板','版本 V1.0｜2026-10-25｜两个数据页均为空白，可直接填写'],
['使用范围','这是后续标准导入功能的建议模板；当前尚未修改程序，不代表现有系统已能直接导入。'],
['填写顺序','先填“监管对象”，再填“联系人”；数据从第2行开始，不改列名、不合并单元格。预设500行填写区。'],
['一行的含义','监管对象：一行一个实际场所；联系人：一行一个联系人与场所的关联。部门、社区联络员另建名册。'],
['已有对象','对象编码必须从系统导出后原样保留，不用原表序号替代；临时关联编号留空。'],
['新增对象','对象编码留空；临时关联编号必填且本文件内唯一，例如 NEW001。联系人页填写相同临时编号。'],
['关联示例（虚构）','监管对象页：对象编码空、临时编号 NEW001、对象名称 示例小区；联系人页：对象编码空、临时编号 NEW001、姓名 示例联系人。不要把示例当正式数据。'],
['新增对象必填','临时关联编号、对象名称、对象类型；地址尽量补齐。类别不清楚选“待确认”。'],
['联系人必填','对象编码与临时关联编号二选一；联系人姓名、联系电话、是否主要联系人必填；同一对象最多一位主要联系人。'],
['对象类型','下拉为建议分类：前四类对应历史导入类别，住宅小区、其他企业、其他场所及待确认需后续与系统字典确认。普通小区不可自动认定高层住宅。'],
['行业类别与管理单位','行业类别填写工贸、仓储、危化品运输经营等；小区对象名称填小区，物业公司填所属/管理单位。'],
['风险等级','本次未确定系统风险字典；暂留空或选“待确认”。不要把原类别中的A/B字母自动转换为风险等级。'],
['所属网格','仅填系统标准网格名称或编码，不根据地址自行猜测。'],
['电话与编号','已设置文本格式。座机、分机按原样保留；一格一个电话号码，多位联系人拆行。不要填写真实示例号码。'],
['资料日期','填写资料实际更新日期，例如2026-10-25，不自动用文件创建日期替代。'],
['坐标','经度纬度同时填写并注明WGS84、GCJ02或BD09；缺失留空，不能填0或凭空补坐标。'],
['原表整理','将横向多组名单纵向展开；仅在原合并单元格覆盖范围内补齐类别；移除标题、合计、空白分隔行。'],
['空值与更新规则','空白建议表示保留原值；不得用空白自动清空旧资料。明确清空、字段覆盖与疑似重复需未来导入预览处理。'],
['去重与删除','按对象编码精确更新；无编码按名称和地址提示疑似重复，不自动合并。此次文件缺少的旧对象不自动删除。'],
['联系人角色','原表仅写“负责人”时就填负责人，不自行升级为法人或消防安全责任人。'],
['扩展行数','超过500条时复制末行格式与下拉校验继续填写；不要把示例或说明粘到数据页。'],
['后续系统流程','下载/导出模板 → 填写 → 上传校验 → 预览新增/更新/冲突 → 确认导入并保留批次日志。']
];
guide.showGridLines=false;guide.tabColor='#D39636';guide.getRange('A1:B22').values=rows;guide.getRange('A1:B22').format.font.name='Microsoft YaHei';guide.getRange('A1:B22').format.font.size=11;guide.getRange('A:A').format.columnWidth=24;guide.getRange('B:B').format.columnWidth=105;guide.getRange('A1:B22').format.wrapText=true;guide.getRange('A1:B22').format.verticalAlignment='center';guide.getRange('A1:B22').format.rowHeight=47;guide.getRange('A1:B1').format.fill='#164B59';guide.getRange('A1:B1').format.font.color='#FFFFFF';guide.getRange('A1:B1').format.font.bold=true;guide.getRange('A2:B2').format.fill='#FFF0D4';for(let i=3;i<=22;i+=2)guide.getRange(`A${i}:B${i}`).format.fill='#F0F6F7';guide.freezePanes.freezeRows(1);
wb.recalculate();
console.log((await wb.inspect({kind:'sheet',include:'id,name',maxChars:1500})).ndjson);
const out='D:/javaprogram/FireControl/outputs/regulatory-template';
await (await SpreadsheetFile.exportXlsx(wb)).save(out+'/监管对象标准导入模板_V1.xlsx');
for(const [s,r]of [['监管对象','A1:G6'],['联系人','A1:G6'],['填写说明','A1:B8']]){const png=await wb.render({sheetName:s,range:r,scale:1.4,format:'png'});await fs.writeFile(out+'/'+s+'.png',new Uint8Array(await png.arrayBuffer()));}
console.log('TEMPLATE_EXPORTED');
