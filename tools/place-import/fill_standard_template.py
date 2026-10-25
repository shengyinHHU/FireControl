"""按列名将资料表填入重点场所模板，保留源文件，不去重、不补造数据。"""
import argparse, hashlib, json, os, posixpath, re, subprocess, tempfile, zipfile
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT = Path(__file__).resolve().parents[2]
NS = {'m': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}

def read_xlsx(path):
    with zipfile.ZipFile(path) as z:
        shared = []
        if 'xl/sharedStrings.xml' in z.namelist():
            shared = [''.join(t.text or '' for t in si.findall('.//m:t', NS)) for si in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('m:si', NS)]
        book = ET.fromstring(z.read('xl/workbook.xml'))
        sheet = book.find('m:sheets/m:sheet', NS)
        rid = sheet.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id')
        rels = ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))
        target = next(r.get('Target') for r in rels if r.get('Id') == rid)
        target = target.lstrip('/') if target.startswith('/') else posixpath.normpath('xl/' + target)
        doc = ET.fromstring(z.read(target))
        cells = {}
        for c in doc.findall('m:sheetData/m:row/m:c', NS):
            if c.find('m:f', NS) is not None:
                raise ValueError('存在公式，请先另存为数值再运行：' + c.get('r'))
            v = c.find('m:v', NS)
            value = (v.text or '') if v is not None else ''.join(t.text or '' for t in c.findall('m:is//m:t', NS))
            if c.get('t') == 's':
                value = shared[int(value)]
            if c.get('t') == 'e':
                raise ValueError('存在 Excel 错误值：' + c.get('r'))
            cells[c.get('r')] = value
        headers = [(re.match(r'[A-Z]+', ref)[0], v.strip()) for ref, v in cells.items() if re.fullmatch(r'[A-Z]+1', ref) and v.strip()]
        names = [v for _, v in headers]
        return sheet.get('name'), cells, headers, doc

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source', type=Path, default=ROOT / 'outputs/regulatory-geocoded/监管对象_公开地址补查定位.xlsx')
    p.add_argument('--template', type=Path, default=Path('E:/software/重点场所标准模板.xlsx'))
    p.add_argument('--output', type=Path, default=ROOT / 'outputs/place-import-ready/重点场所标准模板_已填数据.xlsx')
    a = p.parse_args()
    a.source, a.template, a.output = a.source.resolve(), a.template.resolve(), a.output.resolve()
    if a.output in (a.source, a.template):
        p.error('输出文件不能覆盖资料表或空白模板')
    before = [sha(a.source), sha(a.template)]
    _, src, source_headers, _ = read_xlsx(a.source)
    sheet, template_cells, headers, _ = read_xlsx(a.template)
    required = [name for _, name in headers]
    if len(set(required)) != len(required):
        raise ValueError('模板表头重复')
    for name in required:
        if sum(n == name for _, n in source_headers) > 1:
            raise ValueError('资料表中的标准字段重复：' + name)
    source_map = {name: col for col, name in source_headers}
    missing = [name for _, name in headers if name not in source_map]
    if missing:
        raise ValueError('资料表缺少模板字段：' + '、'.join(missing))
    if '对象名称' not in source_map:
        raise ValueError('缺少对象名称列')
    # 只按名称非空筛选记录；不改名称、类别、地址，也不自行选用候选坐标。
    source_rows = sorted({int(re.search(r'\d+', r)[0]) for r in src if int(re.search(r'\d+', r)[0]) > 1})
    rows, skipped = [], []
    for r in source_rows:
        if not src.get(f'{source_map["对象名称"]}{r}', '').strip():
            if any(src.get(f'{col}{r}', '') for col, _ in source_headers):
                skipped.append(r)
            continue
        row = []
        for _, name in headers:
            value = src.get(f'{source_map[name]}{r}', '')
            if name in ('经度', '纬度') and value != '':
                try:
                    value = float(value)
                except ValueError:
                    pass  # 原样保留异常坐标，供系统预检提示。
            row.append(value if value != '' else None)
        rows.append(row)
    if not rows:
        raise ValueError('资料表中没有可填入的场所')
    a.output.parent.mkdir(parents=True, exist_ok=True)
    runtime = Path(os.environ.get('FIRE_SPREADSHEET_RUNTIME', str(Path.home() / '.cache/codex-runtimes/codex-primary-runtime/dependencies')))
    node = runtime / 'node/bin/node.exe'
    if not node.exists():
        raise RuntimeError('找不到工作簿运行环境，请设置 FIRE_SPREADSHEET_RUNTIME')
    junction = Path(__file__).parent / 'node_modules'
    if not junction.exists():
        q = lambda s: str(s).replace("'", "''")
        subprocess.run(['powershell', '-NoProfile', '-Command', f"New-Item -ItemType Junction -Path '{q(junction)}' -Target '{q(runtime / 'node/node_modules')}' | Out-Null"], check=True)
    values = [[name for _, name in headers]] + rows
    data = {'template': str(a.template), 'output': str(a.output), 'sheet': sheet, 'lastColumn': headers[-1][0], 'values': values}
    with tempfile.TemporaryDirectory(prefix='fill-place-template-') as temp:
        manifest = Path(temp) / 'data.json'
        manifest.write_text(json.dumps(data, ensure_ascii=False, allow_nan=False), encoding='utf-8')
        subprocess.run([str(node), str(Path(__file__).with_suffix('.mjs')), str(manifest)], check=True)
    # 对保存后的文件逐格核对，包括空白、电话号码、类别和坐标。
    _, actual, actual_headers, _ = read_xlsx(a.output)
    assert actual_headers == headers, '输出表头与模板不一致'
    for r, row in enumerate(values, 1):
        for (col, name), expected in zip(headers, row):
            got = actual.get(f'{col}{r}', '')
            if isinstance(expected, float):
                assert abs(float(got) - expected) < 1e-10, f'坐标校验失败 {col}{r}'
            else:
                assert got == (expected or ''), f'内容校验失败 {col}{r}'
    assert before == [sha(a.source), sha(a.template)], '输入文件发生变化'
    coordinate_indices = [i for i, (_, h) in enumerate(headers) if h in ('经度', '纬度')]
    coords = sum(all(row[i] is not None for i in coordinate_indices) for row in rows)
    report = {'records': len(rows), 'with_coordinates': coords, 'without_coordinates': len(rows) - coords, 'verified_cells': len(values) * len(headers), 'skipped_unnamed_source_rows': skipped, 'unmapped_source_headers': [n for _, n in source_headers if n not in dict((n, c) for c, n in headers)], 'input_files_unchanged': True}
    a.output.with_suffix('.核对报告.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False))
    print('已生成：' + str(a.output))

if __name__ == '__main__':
    main()

