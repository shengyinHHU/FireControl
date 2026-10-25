import fs from 'node:fs/promises';
import path from 'node:path';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
const d = JSON.parse(await fs.readFile(process.argv[2], 'utf8'));
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(d.template));
const s = wb.worksheets.getItem(d.sheet);
// Explicitly restore all supplied values, including blanks, after importing template styles.
s.getRange(`A1:${d.lastColumn}${d.values.length}`).values = d.values;
for (const [i, h] of d.values[0].entries()) {
  const col = String.fromCharCode(65 + i); // Standard template has 21 columns (A:U).
  s.getRange(`${col}2:${col}${d.values.length}`).setNumberFormat(['经度', '纬度'].includes(h) ? '0.0000000' : '@');
}
s.freezePanes.freezeRows(1);
wb.recalculate();
await (await SpreadsheetFile.exportXlsx(wb)).save(d.output);
const preview = await wb.render({sheetName: d.sheet, range: 'A1:F6', scale: 1.2, format: 'png'});
await fs.writeFile(path.join(path.dirname(d.output), '填表预览.png'), new Uint8Array(await preview.arrayBuffer()));
console.log('Workbook exported.');
