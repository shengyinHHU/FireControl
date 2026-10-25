import fs from 'node:fs/promises';
import path from 'node:path';
import {Workbook,FileBlob,SpreadsheetFile} from '@oai/artifact-tool';
const data=JSON.parse(await fs.readFile(process.argv[2],'utf8'));
const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(data.template));
const s=wb.worksheets.getItem('监管对象');
if(!s.getRange('F1').values[0][0])s.getRange('F1').values=[['所属/管理单位']];
if(s.getRange('A1:U1').values[0].join('|')!=='来源序号|行业类别|对象名称|负责人|联系电话|所属/管理单位|联系人2|联系电话2|联系人3|联系电话3|对象类型|详细地址|所属网格|风险等级|对象编码|经度|纬度|坐标系|资料日期|来源文件|备注')throw Error('模板列名不符，停止转换：'+JSON.stringify(s.getRange('A1:U1').values));
const n=data.records.length;
s.getRange('A1:U501').unmerge();
s.getRange('A1:U501').format.borders={preset:'none'};
s.getRange('A1:U501').format.font.name='Microsoft YaHei';
s.getRange('A1:U501').format.font.size=11;
s.getRange('A1:U501').format.verticalAlignment='center';
s.getRange('A1:U501').format.horizontalAlignment='left';
s.getRange('A1:U501').format.rowHeight=28;
s.getRange('A1:U1').format.rowHeight=36;
s.getRange('A1:U1').format.fill='#174F5C';
s.getRange('A1:U1').format.font.color='#FFFFFF';
s.getRange('B:B').format.columnWidth=38;
s.getRange(`A2:U${n+1}`).values=data.records.map(r=>r.values);
s.getRange(`A2:U${n+1}`).format.rowHeight=32;
s.getRange(`A2:U${n+1}`).format.wrapText=false;
s.getRange('W1:X17').clear({applyTo:'contents'});
const notes=[['已转换单页清单',`监管对象 ${n} 条；部门/社区联络员 ${data.excluded.length} 条另存核对报告，未混入场所。`],['当前范围','仅转换Excel，不访问或修改数据库；原表和空白模板未改动。'],['A来源序号','原表序号，仅供溯源；不是系统对象编码。'],['B行业类别 → industry_category','保留原表类别及括号原文；住宅小区原表没有行业类别，留空。'],['C对象名称 → fire_place.name','小区取“小区名称”，其他取“单位”，不把物业公司当作小区。'],['D/E负责人及电话','后续对应fire_place_contact.contact_name/phone；角色原文仅为负责人，contact_role需业务确认，不能自动当法人。'],['F管理单位 → management_company','仅住宅小区填写物业名称；其他场所没有管理单位资料时留空。'],['G:J其他联系人','来源没有其他联系人，全部留空，不复制同一人凑数。'],['K对象类型 → place_type','工贸企业=industry；商业综合体=commercial；养老机构=elderly。住宅小区与待确认尚无确认字典值，须核对。'],['L地址 / M网格','对应address/grid_id；资料未提供场所地址或网格，留空。右侧联络员网格不擅自关联场所。'],['N风险等级','没有可靠对应资料，不从A/B分级推断。'],['O对象编码 → place_id','当前没有已核实的数据库ID，全部留空；历史同名仅标记候选，不能当新增直接导入。'],['P:R坐标','来源无坐标，留空。后续入库/地图需要核对并转换为系统GCJ02。'],['S资料日期','原表标题/工作表日期不一致且无逐条日期，留空待确认。'],['T来源文件 / U备注','来源文件名、原始单元格范围及问题标记；完整原始值、SHA256和块标识保存在核对报告。'],['历史去重说明',`有${data.summary.historical_name_matches}条与项目历史导入快照同名；只做精确去空白比较，不代表实时数据库匹配结果。`],['如何重跑','运行 tools/place-import/convert_lists.py；数据库字段映射及运行命令见同目录README.md。']];
s.getRange('W1:X17').values=notes;s.getRange('W1:X17').format.wrapText=true;s.getRange('W1:X17').format.rowHeight=76;
wb.recalculate();
const check=s.getRange(`A2:U${n+1}`).values;if(check.length!==n||check.some((r,i)=>r[2]!==data.records[i].values[2]||r[4]!==data.records[i].values[4]))throw Error('写入记录核对失败');
console.log((await wb.inspect({kind:'table',range:'监管对象!A1:F4',tableMaxRows:4,tableMaxCols:6,maxChars:1600})).ndjson);
const output=path.join(data.output_dir,'监管对象_已整理_单页.xlsx');await(await SpreadsheetFile.exportXlsx(wb)).save(output);
const p=await wb.render({sheetName:'监管对象',range:'A1:F7',scale:1.2,format:'png'});await fs.writeFile(path.join(data.output_dir,'预览.png'),new Uint8Array(await p.arrayBuffer()));
console.log('SAVED '+output);



