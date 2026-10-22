# 119警情数据库设计与导入说明

## 导入范围

执行 SQL/fire_119_incident.sql，目标为现有 dachang 数据库。MySQL 5.7.8+/8.0，InnoDB、utf8mb4、JSON。仅创建4张空表，不导入 result.json，不修改现有业务表，不创建机构、字典、权限或任务，不调用外部接口。不同库名需先修改 USE。DDL 会隐式提交，请使用遇错即停止的导入方式。

重复执行不会清空或覆盖已有数据，但 CREATE TABLE IF NOT EXISTS 不会自动升级不一致的同名表。本文件是初次建表脚本，不是后续版本迁移脚本。

## 数据规模与实际响应

依据 tools/119-secret-key/result.json：code=200，pageNum=1，pageSize=20，当前文件只有20条；接口报告 total=2265、pages=114、hasNextPage=true。2265是该查询范围在当时的总量，不是大厂街道的记录数，也不代表每天新增量。只凭这一页不能估算日增长、保留周期或最终存储占用。

当前采用单库普通表及复合索引，无需分库分表或分区。历史只在源内容变化时追加，重复轮询仅更新 last_seen_at，不生成重复历史。成功分页不重复保存整个响应，源记录由主表及必要历史保存。后续根据新增量、变更频率、历史/日志保留周期再确定归档策略。

来源每增加一条警情，对应一条当前快照；不同状态更新不会产生新的主表警情。同一来源和外部ID唯一，外部ID区分大小写。source_system 为后端确定的稳定接入标识，测试、生产应不同；不把 orderSource 或区域编码当作全局警情唯一标识。

## 表职责

- fire_119_incident：当前警情快照，外部源字段与本地归属字段分开。
- fire_119_incident_history：源记录各个变化版本，首次入库为版本1；重复响应不追加，同一内容变化回来仍可产生新版本。
- fire_119_sync_state：按来源和实际查询区划记录整轮结果；没有新增可不代表没有旧警情状态变化。
- fire_119_sync_log：整轮、分页和认证摘要；认证禁止保存请求密码、secretKey、Token及响应原文。若依 sys_job_log 可继续用于调度执行摘要，本表记录分页和落库详情。

各表逻辑关联，不依赖导入顺序之外的外部表。place_id、dept_id、supervisor_user_id 为可空逻辑关联；后续使用若依数据权限，不能因为没有关联机构就默认所有人可见。

## 原字段映射

| 接口字段 | 主表字段 |
| --- | --- |
| id | external_id |
| xzqybh | region_code |
| ajlxdm / ajlxmc | case_type_code / case_type_name |
| ajztdm / ajztmc | case_status_code / case_status_name |
| lasj | alarm_time |
| afdz / ajms | address / description |
| ajxz / ajxzmc | case_nature_code / case_nature_name |
| hbzt / hbztmc | merge_status_code / merge_status_name |
| temp_ajdjdm / temp_ajdjdmmc | case_level_code / case_level_name |
| jd / wd | source_longitude / source_latitude |
| sjc | source_sjc |
| create_time | source_create_time_utc |
| njs_create_time / njs_update_time | source_njs_create_time_utc / source_njs_update_time_utc |
| sugon_create_time / sugon_update_time | source_sugon_create_time_utc / source_sugon_update_time_utc |
| manually_add / counterfeit / flag / manual_operate_count | source_manually_add / source_counterfeit / source_flag / source_manual_operate_count |
| 单条完整对象 | raw_json |

实际返回的 ajxz、hbzt、temp_ajdjdm 是数值，而文档写字符串；代码类字段统一转字符串，raw_json保留原类型。布尔值原样映射为0/1，含义不明确的 flag、counterfeit 等不用于过滤、删除或判断虚警。ajztmc以源名称为准，不用本地字典擅自覆盖。

## 大厂街道范围

当前响应含横梁、竹镇、雄州等其他街道地址，没有明确街道字段。不能把 query_region_code=320116 认定为大厂街道，更不能为全部记录填写大厂街道。所有类型均可入库，展示范围由独立归属字段决定。

新记录默认 street_match_status=pending；前端大厂街道查询应只返回 inside。归属需官方街道字段/筛选能力、人工核验或已确认坐标系的权威街道边界判定，具体方式尚未确定；未知时允许待判定，不会凭关键词认定。若地址或源坐标变化，应重新判定街道归属，不能永久沿用旧结论。此字段不等同部门数据权限。

## 地图与时间

源经纬度保留8位小数；接口坐标系未知，source_coord_system默认unknown。map_longitude/map_latitude只存已核实GCJ02的展示坐标；未确认时不填，coordinate_status=pending。经度须在[-180,180]、纬度须在[-90,90]，应用校验成对存在，缺失或异常保留raw_json，不以0,0补点。坐标变化时必须重算/清空旧展示坐标。

实际ISO时间含+0000，如2026-10-21T05:25:31.000+0000。名称带_utc的列必须解析显式偏移后保存UTC，不能直接当北京时间；本地审计、同步时间统一Asia/Shanghai。lasj、sjc没有时区，暂保留原墙上时间，源时区需接口方确认。raw_json完整保留原时间字符串及毫秒。

虽然返回中存在多种更新时间字段，接口没有提供更新时间过滤参数；字段业务语义、可靠性及哪一个可以作增量游标仍待确认。不能据此实现只查第一页或只按最大更新时间截断扫描。排序按来警时间时，要考虑旧案件状态回查和分页期间新增引起的漂移。

## 后续实时入库约定

本阶段没有实现同步程序。后续每次接口请求成功后实时落库：先校验分页及记录，按(source_system,external_id)新增/更新；规范化源JSON后计算SHA256，忽略JSON属性顺序，保留数组顺序。不得将本地last_seen_at、归属核验、备注等加入源hash。

同一记录主表更新、版本递增及历史追加必须在同一事务内执行，并防止同一来源并发同步覆盖。源同步只覆盖外部字段，不覆盖人工归属、部门、负责人、备注。某页/某行失败只记失败，不把整轮标为完整成功。接口暂时查不到记录时不自动删除或结案，数字状态不按大小判断先后。

后续所有案件类型均获取和保存，包括火警扑救、抢险救援、社会救助及未知类型；虚警、测试、合并状态也保留。呈现时保留原类别和状态，不统一转换成设备火警的“待核验”。不同来源聚合时使用带来源的前端ID，分页排序由后端完成。

## 当前导入后的结果

四张表均应为空。导入不会改变现有大屏；后续还需要完成Token管理、定时分页请求、街道归属判定、事务落库、权限查询及前端接入。接口凭据放在后端配置/环境中，SQL不包含任何凭据。

## 后续实现状态更新 2026年10月21日

后端每分钟请求、事务入库、边界筛选及前端统一警情查询已实现，部署配置和运行说明见 fire-119-access.md。用户已确认119源坐标与文档边界均为GCJ02，原文关于坐标系未确认的描述属于设计阶段记录，以本更新为准。脚本四表结构未改变，不需要新增迁移。

## 同步优化更新（2026年10月22日）

现已改为每分钟前3页快速扫描、每10分钟完整核对；每页批量查哈希和带哈希条件的元数据更新。只对新记录/内容变化独立事务写主表和历史，保留人工归属与事务回滚。SQL/fire_119_sync_optimization.sql 新增快速扫描状态和新记录发现时间六字段；旧版本必须升级，当前本地库已升级。以 fire-119-access.md 的最新运行说明为准。
