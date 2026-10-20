"""Generate the reviewed FireControl seed SQL from the four supplied workbooks.
Requires openpyxl and xlrd. Set FIRE_SHEET_DEPS if these are in a separate directory.
"""
import os, sys, json, re, hashlib
from pathlib import Path
from decimal import Decimal, InvalidOperation
from collections import Counter
if os.environ.get('FIRE_SHEET_DEPS'):
    sys.path.insert(0, os.environ['FIRE_SHEET_DEPS'])
import openpyxl, xlrd
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'SQL'
SOURCES = [
 ('2608大厂养老院重点监管企业底数清单.xlsx', 'elderly'),
 ('高层住宅重点监管单位汇总清单.xls', 'highrise'),
 ('工贸企业重点监管单位汇总清单(3).xls', 'industry'),
 ('商业综合体重点企业汇总清单.xls', 'commercial'),
]
def text(v):
    if v is None: return None
    if isinstance(v, float) and v.is_integer(): v = int(v)
    s = str(v).strip()
    return s or None

def lit(v):
    # Hex UTF-8 literals are independent of NO_BACKSLASH_ESCAPES/sql_mode.
    if v is None: return 'NULL'
    if isinstance(v, (int, Decimal)): return str(v)
    return "''" if str(v) == "" else "(CONVERT(0x" + str(v).encode('utf-8').hex() + " USING utf8mb4) COLLATE utf8mb4_general_ci)"

def number(v, field, issues):
    s = text(v)
    if s in (None, '/', '／', '-', '—'): return None
    if field == '建筑面积':
        s = re.sub(r'\s*(㎡|平方米|m²|m2)\s*$', '', s, flags=re.I).replace(',', '')
    try:
        d = Decimal(s)
        if not d.is_finite() or d < 0: raise ValueError()
        if field != '建筑面积' and d != d.to_integral_value(): raise ValueError()
        return d if field == '建筑面积' else int(d)
    except (InvalidOperation, ValueError):
        issues.append(field + ': 无法规范化，原值保留在来源行')
        return None

def add_row(table, data, condition):
    return 'INSERT INTO `' + table + '` (' + ', '.join('`'+k+'`' for k in data) + ', `create_time`)\nSELECT ' + ', '.join(lit(v) for v in data.values()) + ', NOW()\nWHERE NOT EXISTS (' + condition + ');'

DDL = '''-- 消防监管场所增量建表与全量初始数据。MySQL 5.7.8+/8.0，utf8mb4。
-- 前置条件：dachang 中已安装若依 sys_dict_type / sys_dict_data 等系统表。
-- 不执行 DROP/TRUNCATE，不修改现有若依结构，不建立登录账号，不调用地图 API。
-- 相同来源文件/工作表/行号只导入一次，重跑不覆盖人工修改。
-- 文件内容更新后按新来源导入，不自动按名称合并，请先复核去重。
-- DDL 会隐式提交；后续字典与数据 DML 在事务中执行。请使用遇错即停止的客户端。
SET NAMES utf8mb4;
USE `dachang`;

CREATE TABLE IF NOT EXISTS `fire_place` (
 `place_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '监管场所ID',
 `source_key` char(64) DEFAULT NULL COMMENT '初始导入来源SHA256，手工新增可空',
 `name` varchar(255) NOT NULL COMMENT '企业、机构或小区名称',
 `place_type` varchar(32) NOT NULL COMMENT '字典fire_place_type的dict_value',
 `industry_category` varchar(100) DEFAULT NULL COMMENT '原表行业类别',
 `management_company` varchar(255) DEFAULT NULL COMMENT '物业或管理单位',
 `credit_code` varchar(18) DEFAULT NULL COMMENT '经营主体统一社会信用代码，非场所唯一标识',
 `dept_id` bigint(20) DEFAULT NULL COMMENT '监管归属部门，逻辑关联sys_dept',
 `supervisor_user_id` bigint(20) DEFAULT NULL COMMENT '监管人员，逻辑关联sys_user',
 `community_name` varchar(100) DEFAULT NULL COMMENT '所属社区',
 `grid_id` bigint(20) DEFAULT NULL COMMENT '真实业务网格，后续接入',
 `address` varchar(500) DEFAULT NULL COMMENT '原始地址',
 `normalized_address` varchar(500) DEFAULT NULL COMMENT '核实补全后的定位地址',
 `adcode` varchar(12) DEFAULT NULL COMMENT '行政区划代码',
 `main_risk_sources` text COMMENT '原表主要风险源',
 `risk_level` varchar(16) DEFAULT NULL COMMENT '后续评估风险等级，不从风险源猜测',
 `employee_count` int unsigned DEFAULT NULL COMMENT '原表企业人数，非居民人数',
 `building_area_m2` decimal(14,2) DEFAULT NULL COMMENT '建筑面积，原数值按平方米，待业务核实单位',
 `resident_elderly_count` int unsigned DEFAULT NULL COMMENT '居住老人数',
 `disabled_elderly_count` int unsigned DEFAULT NULL COMMENT '失能老人数',
 `highrise_building_count` int unsigned DEFAULT NULL COMMENT '高层建筑楼栋数',
 `passenger_flow_raw` varchar(255) DEFAULT NULL COMMENT '客流量原文，周期尚未明确',
 `mini_fire_station_equipment` text COMMENT '微型消防站器材原文',
 `emergency_team_count` int unsigned DEFAULT NULL COMMENT '应急消防队伍人数',
 `longitude` decimal(10,7) DEFAULT NULL COMMENT 'GCJ02经度，未知留空',
 `latitude` decimal(10,7) DEFAULT NULL COMMENT 'GCJ02纬度，未知留空',
 `coordinate_system` varchar(16) NOT NULL DEFAULT 'GCJ02' COMMENT '地图输出坐标系',
 `coordinate_source` varchar(32) DEFAULT NULL COMMENT 'amap_geocode/manual/imported',
 `location_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '字典fire_location_status',
 `matched_address` varchar(500) DEFAULT NULL COMMENT '地址解析匹配结果',
 `amap_poi_id` varchar(100) DEFAULT NULL COMMENT '高德POI标识',
 `location_checked_by` bigint(20) DEFAULT NULL COMMENT '核验人员，逻辑关联sys_user',
 `location_checked_at` datetime DEFAULT NULL COMMENT '坐标核验时间',
 `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
 `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
 `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
 `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
 `remark` varchar(500) DEFAULT NULL COMMENT '备注',
 PRIMARY KEY (`place_id`),
 UNIQUE KEY `uk_fire_place_source` (`source_key`),
 KEY `idx_fire_place_filter` (`place_type`,`del_flag`,`status`,`location_status`),
 KEY `idx_fire_place_dept` (`dept_id`),
 KEY `idx_fire_place_supervisor` (`supervisor_user_id`),
 KEY `idx_fire_place_grid` (`grid_id`),
 KEY `idx_fire_place_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消防监管场所';

CREATE TABLE IF NOT EXISTS `fire_place_contact` (
 `contact_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '联系人记录ID',
 `place_id` bigint(20) NOT NULL COMMENT '场所ID',
 `contact_role` varchar(32) NOT NULL COMMENT '字典fire_contact_role',
 `contact_name` varchar(100) DEFAULT NULL COMMENT '联系人姓名',
 `phone` varchar(64) DEFAULT NULL COMMENT '联系电话，字符串保存',
 `sort_order` int NOT NULL DEFAULT 0 COMMENT '同角色联系人排序',
 `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
 `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
 `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
 `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
 `remark` varchar(500) DEFAULT NULL COMMENT '备注',
 PRIMARY KEY (`contact_id`),
 UNIQUE KEY `uk_fire_contact_role` (`place_id`,`contact_role`,`sort_order`),
 CONSTRAINT `fk_fire_contact_place` FOREIGN KEY (`place_id`) REFERENCES `fire_place` (`place_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='场所联系人，非登录账号';

CREATE TABLE IF NOT EXISTS `fire_place_import_row` (
 `import_row_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '来源记录ID',
 `source_key` char(64) NOT NULL COMMENT '文件哈希、工作表、行号计算的SHA256',
 `batch_no` varchar(64) NOT NULL COMMENT '导入批次',
 `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
 `file_sha256` char(64) NOT NULL COMMENT '原始文件内容SHA256',
 `sheet_name` varchar(100) NOT NULL COMMENT '原始工作表',
 `row_number` int unsigned NOT NULL COMMENT 'Excel实际行号',
 `source_serial` varchar(64) DEFAULT NULL COMMENT '原始序号',
 `raw_data` json NOT NULL COMMENT '按列号保存标题、表头、原值，保留重复列名',
 `place_id` bigint(20) DEFAULT NULL COMMENT '关联场所',
 `import_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/imported/review/rejected',
 `error_message` text COMMENT '数据处理说明',
 `create_by` varchar(64) DEFAULT '' COMMENT '导入人',
 `create_time` datetime DEFAULT NULL COMMENT '导入时间',
 PRIMARY KEY (`import_row_id`),
 UNIQUE KEY `uk_fire_import_source` (`source_key`),
 KEY `idx_fire_import_batch` (`batch_no`),
 KEY `idx_fire_import_place` (`place_id`),
 CONSTRAINT `fk_fire_import_place` FOREIGN KEY (`place_id`) REFERENCES `fire_place` (`place_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消防清单原始数据及来源';

START TRANSACTION;
'''
parts=[DDL]
dicts=[('fire_place_type','消防监管场所类别', [('industry','工贸企业'),('highrise','高层住宅'),('commercial','商业综合体'),('elderly','养老机构')]),('fire_location_status','消防场所定位状态',[('pending','待定位'),('review','待核验'),('confirmed','已确认'),('failed','定位失败')]),('fire_contact_role','消防场所联系人角色',[('legal_representative','法人'),('principal','主要负责人'),('onsite_manager','现场负责人')])]
for dt,label,values in dicts:
    parts.append(add_row('sys_dict_type',dict(dict_name=label,dict_type=dt,status='0',create_by='admin',remark='消防业务字典'), 'SELECT 1 FROM sys_dict_type WHERE dict_type='+lit(dt)))
    for i,(value,label) in enumerate(values,1):
        parts.append(add_row('sys_dict_data',dict(dict_sort=i,dict_label=label,dict_value=value,dict_type=dt,css_class='',list_class='default',is_default='N',status='0',create_by='admin',remark='消防业务字典'), 'SELECT 1 FROM sys_dict_data WHERE dict_type='+lit(dt)+' AND dict_value='+lit(value)))

stats=[]; all_rows=[]; issues_all=[]; contact_count=0
for filename,kind in SOURCES:
    p=ROOT/'docs'/filename
    digest=hashlib.sha256(p.read_bytes()).hexdigest()
    if p.suffix=='.xlsx':
        wb=openpyxl.load_workbook(p,data_only=True)
        formula_wb=openpyxl.load_workbook(p,data_only=False)
        assert not any(c.data_type=='f' for s in formula_wb for row in s for c in row), 'Unexpected formula: manual review needed'
        sheets=[(s.title,list(s.values)) for s in wb]
    else:
        wb=xlrd.open_workbook(str(p))
        sheets=[(s.name,[s.row_values(i) for i in range(s.nrows)]) for s in wb.sheets()]
    count=0
    for sn,rows in sheets:
        if not any(any(text(v) is not None for v in r) for r in rows): continue
        headers=rows[1]
        for ri,row in enumerate(rows[2:],3):
            if not any(text(v) is not None for v in row): continue
            assert text(row[1]), (filename,ri,'missing name')
            issues=[]
            def get(h):
                return row[headers.index(h)] if h in headers else None
            def num(h): return number(get(h),h,issues)
            source_key=hashlib.sha256((digest+'\x1f'+sn+'\x1f'+str(ri)).encode()).hexdigest()
            data=dict(source_key=source_key,name=text(row[1]),place_type=kind,
                industry_category=text(get('行业类别')),management_company=text(get('物业公司名称')),
                address=text(get('地址')),main_risk_sources=text(get('主要风险源')),
                employee_count=num('企业人数'),building_area_m2=num('建筑面积'),
                resident_elderly_count=num('居住老人数'),disabled_elderly_count=num('失能老人数'),
                highrise_building_count=num('高层建筑楼栋数'),passenger_flow_raw=text(get('客流量')),
                mini_fire_station_equipment=text(get('微型消防站器材')),emergency_team_count=num('应急消防队伍人数'),
                create_by='admin',remark='Excel初始导入；坐标及监管归属待核实')
            assert len(data['name'])<=255
            parts.append('-- '+filename+' / '+sn+' / row '+str(ri))
            parts.append(add_row('fire_place',data,'SELECT 1 FROM fire_place WHERE source_key='+lit(source_key)))
            parts.append('SET @fire_place_id = (SELECT place_id FROM fire_place WHERE source_key='+lit(source_key)+');')
            for h,role in [('法人','legal_representative'),('主要负责人','principal'),('现场负责人','onsite_manager')]:
                if h not in headers: continue
                j=headers.index(h); n=text(row[j]); phone=text(row[j+1])
                if n in ('/','／','-','—'): n=None
                if phone in ('/','／','-','—'): phone=None
                if n is None and phone is None: continue
                assert phone is None or len(phone)<=64
                cols='place_id, contact_role, contact_name, phone, sort_order, create_by, create_time'
                parts.append('INSERT INTO fire_place_contact ('+cols+') SELECT @fire_place_id, '+', '.join(lit(v) for v in [role,n,phone,0,'admin'])+', NOW() WHERE NOT EXISTS (SELECT 1 FROM fire_place_contact WHERE place_id=@fire_place_id AND contact_role='+lit(role)+' AND sort_order=0);')
                contact_count+=1
            raw={'title':list(rows[0]),'cells':[{'column':i+1,'header':headers[i] if i<len(headers) else None,'value':v} for i,v in enumerate(row)]}
            raw_text=json.dumps(raw,ensure_ascii=False,separators=(',',':'))
            json.loads(raw_text)
            # Keep the variable unquoted, while all source values still use hex literals.
            src=dict(source_key=source_key,batch_no='excel_initial_'+digest[:16],file_name=filename,file_sha256=digest,sheet_name=sn,row_number=ri,source_serial=text(row[0]),raw_data=raw_text,import_status='review' if issues else 'imported',error_message='；'.join(issues) or None,create_by='admin')
            stmt=add_row('fire_place_import_row',src,'SELECT 1 FROM fire_place_import_row WHERE source_key='+lit(source_key))
            stmt=stmt.replace(', `create_time`)\nSELECT', ', `place_id`, `create_time`)\nSELECT').replace(', NOW()\nWHERE', ', @fire_place_id, NOW()\nWHERE')
            parts.append(stmt)
            all_rows.append({'source_key':source_key,'file':filename,'sheet':sn,'row':ri,'type':kind,'data':data,'raw':raw})
            if issues: issues_all.append({'file':filename,'sheet':sn,'row':ri,'issues':issues})
            count+=1
    stats.append({'file':filename,'sha256':digest,'place_type':kind,'records':count})
parts.append('COMMIT;\n-- 仅统计本次来源，不包含数据库已有的其他记录。')
keys=','.join(lit(x['source_key']) for x in all_rows)
parts.append('SELECT place_type, COUNT(*) AS imported_count FROM fire_place WHERE source_key IN ('+keys+') GROUP BY place_type;')
parts.append('SELECT COUNT(*) AS source_row_count FROM fire_place_import_row WHERE source_key IN ('+keys+');')
parts.append('SELECT COUNT(*) AS contact_count FROM fire_place_contact WHERE place_id IN (SELECT place_id FROM fire_place WHERE source_key IN ('+keys+'));')
assert len(all_rows)==101
assert len(set(x['source_key'] for x in all_rows))==101
csv_files=[str(p.relative_to(ROOT)) for p in ROOT.rglob('*.csv') if not any(x in p.parts for x in ['node_modules','.git'])]
assert not csv_files, 'New CSV found; review mapping before generation: '+repr(csv_files)
(OUT/'fire_place_init.sql').write_text('\n\n'.join(parts)+'\n',encoding='utf-8')
report={'files':stats,'place_count':len(all_rows),'contact_count':contact_count,'source_row_count':len(all_rows),'csv_files_found':csv_files,'normalization_issues':issues_all,'coordinates':'all NULL / pending; source files do not contain coordinates','names_shared_across_rows':[{'name':n,'count':c} for n,c in Counter(x['data']['name'] for x in all_rows).items() if c>1]}
(OUT/'fire_place_import_report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))