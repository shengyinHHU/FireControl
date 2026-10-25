# 两份名单转换为单页模板

运行（项目根目录）：

```powershell
python tools/place-import/convert_lists.py
```

脚本使用 Python 标准库读取来源 XLSX，使用 Codex 捆绑 Node/@oai/artifact-tool 写入 Excel。运行环境默认位于用户目录 `.cache/codex-runtimes/codex-primary-runtime/dependencies`，可用 `FIRE_SPREADSHEET_RUNTIME` 指定。未安装此运行时的机器不能只凭 Python 独立生成；脚本会报错而不会修改来源文件。

输入：docs/企业重点监管对象.xlsx、docs/消防重点监管对象.xlsx；格式与当前核对版本一致。空白模板：outputs/regulatory-template/监管对象标准导入模板_单页版_V2.xlsx。支持 --docs 和 --output-dir 指定目录。输出文件会在同名目录覆盖重建，请先另存人工修改结果。

输出：outputs/regulatory-filled/监管对象_已整理_单页.xlsx（只有一张表）；转换核对报告.json（逐条原文、文件哈希、表名、行号、列块、排除原因、历史候选）；非场所联络员.csv（排除的部门/网格人员，可用Excel打开）。不连接数据库，不执行SQL。

## 原表转换

- 企业表 A:E、F:J、K:O、P:T 四块依次纵向展开。类别只按真实合并范围补齐，保留括号原文。
- 消防表 A:E 和 F:J 的住宅段：小区名称→对象名称，物业名称→管理单位，负责人/电话不变。
- 消防表 F:J 第24行切换表头，后续分别按商业综合体、养老机构解析。
- 消防表 K:O 部门联络员和社区网格联络员不属于监管场所，单独输出，不关联猜测出的场所。
- 不合并同名记录，不补造地址、坐标、风险等级、系统ID和日期。标题日期存在歧义，资料日期留空。
- 来源定位包含列块。同一Excel行有多个场所，不能仅按文件/行号算唯一来源键。报告新键包含文件哈希、表名、行号及列块，不替代现有数据库键。

## 与现有数据库的对应（后续导入时使用）

| 单页列 | 数据库字段/处理 |
|---|---|
| 来源序号 | fire_place_import_row.source_serial，不是place_id |
| 行业类别 | fire_place.industry_category |
| 对象名称 | fire_place.name |
| 负责人 / 联系电话 | fire_place_contact.contact_name / phone；contact_role必须核对，不自动当法人或主要负责人 |
| 所属/管理单位 | fire_place.management_company |
| 联系人2/3与电话 | 后续展开为多条fire_place_contact，并核对角色和sort_order |
| 对象类型 | 工贸→industry，商业→commercial，养老→elderly；住宅小区不能默认映射highrise；住宅/待确认须先定字典 |
| 详细地址 | fire_place.address；本次未提供，留空 |
| 所属网格 | 先解析真实网格ID再写grid_id，不能把文字直接写入ID |
| 风险等级 | fire_place.risk_level，留空 |
| 对象编码 | 经核实的fire_place.place_id，留空，绝不写来源序号 |
| 坐标/坐标系 | 经核验与转换后使用系统GCJ02；本次留空 |
| 来源文件/备注 | 来源表与remark；逐条原值在报告中 |
| 资料日期 | 现有场所表无对应业务日期列，不冒充create_time；后续保存到来源raw_data或明确新增字段 |

历史匹配只对照 SQL/geocoding/places_input.json 的静态快照，去空白后精确同名作为提示。未查询实时数据库，未按名称自动写入已有ID，也不把旧地址当新资料填入。

## 当前Excel高德坐标查询

不再读取最初转换报告里的旧名称，直接读取当前Excel的C对象名称、L详细地址。原工作簿不覆盖，新结果写入outputs/regulatory-geocoded。只向高德发送名称/地址/南京城市限定，联系人资料不发送。

```powershell
python tools/place-import/geocode_workbook.py
python tools/place-import/select_coordinates.py
& "$env:USERPROFILE/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe" tools/place-import/write_coordinates.mjs outputs/regulatory-geocoded/writeback.json
```

第一步沿用SQL/geocoding/run_js_geocoding.py和已有官方JS示例。当前仅新增--output参数以隔离输出，既有101家结果不覆盖。复用旧缓存；中断后可重跑，只有未缓存查询会联网。离线重新整理可给第一步加--offline。第二步使用精确同名、南京限定且结果没有未见页的POI候选；多候选不强取第一项，全部候选保留review_results.json。没有地址时仍需人工确认候选是否为本场所。

第三步只在输出副本填P:R并追加Y:AE审查字段；其余字段保持当前文件值，原地址不被高德地址覆盖。若已有经纬度任一值，保留原值，不自动覆盖。源文件在查询期间变化时停止写回，需重新开始。未定位条目不是不存在，通常需补完整名称、门牌地址。所有返回坐标为GCJ02候选，不标记已实地核验。不执行生成目录中的任何SQL，不连接业务数据库。

## 大厂街道范围模糊定位（按用户指定规则）

以RuoYi-Vue3/src/assets/geo/dachang-boundary.json的geometry为范围（用户确认GCJ02），使用点在多边形内判断，排除洞，不把矩形当街道。高德DistrictSearch只返回街道名称和中心、未给边界，故使用项目已有边界。

```powershell
python tools/place-import/fuzzy_dachang.py
python tools/place-import/query_dachang.py
python tools/place-import/fuzzy_dachang.py
& "$env:USERPROFILE/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe" tools/place-import/write_coordinates.mjs outputs/regulatory-geocoded/fuzzy_writeback.json
```

基于真实返回候选，忽略行政前缀、公司后缀与括号附注计算名称相似度。最低0.65，取距最佳相似度0.15以内的合理候选；编号村与东西村冲突不匹配。合理候选最大两两距离≤300米，选名称接近且非停车场、出入口等附属点优先的代表点。超过300米不混为一个地址。原名称、电话、地址、备注保持不变。模糊坐标均为待核验代表点，不能当作已确认场所门牌。补充查询关键词为“大厂＋规范化名称”，继续限定南京搜索并按真实街道边界过滤。

复现使用workbook_manifest.json固定的原文件哈希，源文件变化时停止，不混用旧输入。

## 公开企业地址补查（2026-10-25）

本轮对152条未定位记录做公开网页搜索。相关公开来源摘录保存在outputs/regulatory-geocoded/public_address_search.json；没有找到可靠地址不表示企业不存在。没有访问登录后的会员数据，没有绕过验证码。企查查公开页、政府公示、企业官网和目录分别标注来源性质。

review_public_sources.py为人工审阅后确认的29项地址线索（覆盖31行，原表部分企业重复）；不把自动抓到的“地址”字段直接当企业地址，防止误取采购方、网站页脚或总部地址。注册地、旧住所、项目地址不是现场核验位置。

复现本次已审阅证据与缓存：

```powershell
python tools/place-import/review_public_sources.py
python tools/place-import/prepare_public_geocoding.py
python tools/place-import/query_dachang.py --tasks public_address_tasks.json --report public_address_run.json
python tools/place-import/merge_public_addresses.py
& "$env:USERPROFILE/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe" tools/place-import/write_public_coordinates.mjs outputs/regulatory-geocoded/public_writeback.json
python tools/place-import/verify_public_coordinates.py
```

本流程不自动运行网页搜索，review_public_sources.py是本次人工审阅证据，不是实时企业信息API；日后变更应重新核查原链接再调整。高德请求仅未缓存项联网。只保留细粒度地址或精确企业主体POI，过滤至项目确认的街道多边形内；多候选需最大间距≤300米。已定位173条不覆盖。本次新增14条，总187条，剩余138条：17条有地址线索但范围/精度/现址或主体未确认，121条未核实到可靠完整地址。新增含注册地址候选，并不宣称现场已核实。

Excel输出“监管对象_公开地址补查定位.xlsx”，仍只有一张表。P:R是本轮采纳的坐标；AF:AP是地址证据。AK:AL是公开地址的候选点，可能在街道外，不能将其误认为已采纳场所坐标；AO范围、AP处理结论解释是否采纳。原始L详细地址保持不变。输出含联系人资料，按原文件管理。
