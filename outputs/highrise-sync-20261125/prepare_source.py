import hashlib,json
from pathlib import Path
import openpyxl

ROOT=Path(__file__).resolve().parents[2];OUT=Path(__file__).resolve().parent
path=ROOT/'docs/2025年江北新区直管区住宅小区物业管理基础数据汇总1.21.xlsx'
book=openpyxl.load_workbook(path,data_only=True);s=book['大厂']
merged={}
for area in s.merged_cells.ranges:
    value=s.cell(area.min_row,area.min_col).value
    for row in range(area.min_row,area.max_row+1):
        for col in range(area.min_col,area.max_col+1):merged[(row,col)]=value
rows=[]
for row in range(2,s.max_row+1):
    if not s.cell(row,4).value:continue
    cells={s.cell(row,c).coordinate:s.cell(row,c).value for c in range(1,29)}
    fields={s.cell(1,c).value:merged.get((row,c),s.cell(row,c).value) for c in range(1,29)}
    rows.append(dict(row=row,name=s.cell(row,4).value,fields=fields,original_cells=cells))
assert len(rows)==101
payload=dict(file=path.name,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),sheet=s.title,merged_ranges=[str(a) for a in s.merged_cells.ranges],rows=rows)
(OUT/'source.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2,default=str),encoding='utf-8')
print(json.dumps(dict(rows=len(rows),merged_ranges=len(merged),source_hash=payload['sha256']),ensure_ascii=False))
