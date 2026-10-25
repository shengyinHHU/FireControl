# 重点场所一键导入

## 部署

1. 已有fire_place、fire_place_contact、fire_place_import_row基础表（SQL/fire_place_init.sql）。在项目业务库dachang执行 **SQL/fire_place_import.sql**，可重复执行，不修改旧场所、不删除数据。新增持久批次表、串行去重锁、联系人角色、导入权限，并统一四类字典中文名称。
2. 重启新版后端，更新前端。管理员默认可见；其他导入岗位需同时具有fire:places:list、fire:places:import，并按现有岗位授予数据范围。
3. 管理工作台 → 重点场所 → 导入场所 → 上传单页Excel → 检查文件 → 核对新增/重复/错误 → 确认新增。结果可下载。也可下载标准单页模板。

本次开发没有连接业务数据库执行DDL或实际导入；当前Excel已在临时隔离MySQL试导入，验证真实读取/保存路径，并非前端模拟写入。

## 字段

- “场所类别”明确对应fire_place.place_type，不使用“行业类别”猜测。重点企业=industry，高层建筑=highrise，综合商业体=commercial，养老机构=elderly。前端、字典与导入报告统一中文标签；旧标签工贸企业、高层住宅、商业综合体、养老院和原表头对象类型仍兼容，存在场所类别与对象类型双表头则拒绝以免歧义。
- 对象名称→name，行业类别→industry_category，所属/管理单位→management_company，详细地址→address，风险等级→risk_level。
- 经度/纬度/坐标系为唯一地图坐标来源；不是右侧“公开地址候选经度/纬度”等检索结果列。有坐标需成对、有效、非零且GCJ02；写入location_status=review，可在对应开启的地图图层显示，保持待核验。无坐标写pending，不上图，完整保留在重点场所列表。
- 负责人/联系电话→fire_place_contact的import_responsible，联系人2、3→import_contact，不推断为法人。电话按原文保留；不创建系统用户。
- 来源序号不是place_id。带对象编码的行跳过，本功能不更新已有对象。所属网格原文保留在备注与来源审计，不把Excel文字冒充网格ID。
- source_key由文件SHA256、表名、行号生成。全部原单元格（包括公开地址与来源链接）与标准字段保存在fire_place_import_row.raw_data；其他不用于当前业务的列不会丢失。

## 重复与事务

名称去标点、行政前缀和公司后缀，兼容括号附注；检查同名、包含及编辑距离近似，不按公司名字搜索结果第一条直接覆盖。预检扫描现有表（含停用/删除记录，避免重新激活）；管理员可见相似名称，其他岗位只得到泛化提示，避免跨数据范围泄露。文件内相似名称也跳过，先出现的有效新行优先。规则保守，疑似同名需要核对后另行处理，不提供强制覆盖。

确认时在数据库锁内再次读取现有名称，防止两个预检都认为可新增。批次归属当前用户，两小时内有效；客户端仅提交batchId，不能篡改预检行。确认事务包含全部有效场所、联系人、来源和批次完成状态，任一步失败全部回滚，可用同一批重试。完成后重复确认返回原结果，不再次插入。

重要：已有同名场所即使没有坐标，本次Excel带坐标也不会覆盖该旧记录；须另做核对后的补坐标流程。列表新增记录归属当前登录部门和导入人。日期按来源原文留存，不冒充建档时间。

## 接口

POST /fire/places/import/template：单页标准模板下载。
POST /fire/places/import/preview：multipart字段file，返回owner-bound批次、汇总与全部行检查结果。
POST /fire/places/import/confirm：JSON {"batchId":"..."}，返回幂等导入结果。
均需登录及两项权限，文件只支持.xlsx，最多10MB和2000条；表内100列、10000行范围和文本总量有上限；公式不求值，标准业务列公式报错。预检记录含联系人资料，按业务数据管理。

## 验证

- tools/place-import/verify_project.py：独立临时MySQL，SQL迁移重复执行、真实当前Excel325行、批次所有权、过期、幂等、并发去重、事务故障回滚、来源审计、联系人和列表/地图分离。
- RuoYi-Vue3/tests/fire/place-import.browser.cjs：真实Vue/Element Plus + HTTP夹具，文件保留重试、确认保留重试、重复筛选及CSV、完成刷新、重新打开、只读权限、窄屏。
- npm --prefix RuoYi-Vue3 run test:fire：原地图/数据来源等回归。浏览器夹具不代表真实登录/生产数据库联调。
