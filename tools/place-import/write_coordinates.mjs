import fs from 'node:fs/promises';
import path from 'node:path';
import {FileBlob,SpreadsheetFile} from '@oai/artifact-tool';
const data=JSON.parse(await fs.readFile(process.argv[2],'utf8'));
const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(data.input));const s=wb.worksheets.getItem(data.sheet);
s.getRange('A1:X501').values=data.original_values;
s.getRange('Y1:AE1').values=[['定位状态','高德匹配名称','高德匹配地址','定位判断依据','候选数量','匹配级别','查询范围']];
s.getRange('Y1:AE1').format.fill='#174F5C';s.getRange('Y1:AE1').format.font.color='#FFFFFF';s.getRange('Y1:AE1').format.font.bold=true;s.getRange('Y:AE').format.columnWidth=28;s.getRange('AA:AB').format.columnWidth=55;
for(const r of data.rows){const i=r.excel_row,c=r.selected;if(c&&!r.existing){s.getRange(`P${i}:R${i}`).values=[[c.longitude,c.latitude,'GCJ02']];s.getRange(`P${i}:Q${i}`).setNumberFormat('0.0000000');}s.getRange(`Y${i}:AE${i}`).values=[[r.existing?'保留已有坐标':c?'候选坐标待核验':'未定位，需补资料',c?.name||'',c?.matched_address||'',r.reason,r.candidate_count,c?.level||c?.method||'',data.scope||'南京市']];}
wb.recalculate();await(await SpreadsheetFile.exportXlsx(wb)).save(data.output);const png=await wb.render({sheetName:data.sheet,range:'O1:R9',scale:1.5,format:'png'});await fs.writeFile(path.join(path.dirname(data.output),'坐标预览.png'),new Uint8Array(await png.arrayBuffer()));console.log('SAVED '+data.output);


