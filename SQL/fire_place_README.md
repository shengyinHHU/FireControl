# 消防监管场所建表与初始数据

## 执行文件

在已安装若依系统表的 MySQL 连接中，完整执行 `fire_place_init.sql`。脚本使用 `dachang` 数据库，包含建表、字典初始化和全部初始数据，无需再读取 Excel 或安装 Python。

不要为导入这些业务数据而重新执行若依/Quartz 原始初始化脚本。此次生成脚本不执行 DROP、TRUNCATE，不修改系统表结构，也不会创建系统用户。

目标语法为 MySQL 5.7.8 及以上，实际验证环境为 MySQL 8.0.27。请在遇错即停止的客户端中运行，勿使用 mysql --force。DDL 不随后续事务回滚；DML 插入在事务内。若执行时发现错误，停止并 ROLLBACK，排查后重跑。

业务表应首次创建；若数据库已存在不同版本的同名表，CREATE TABLE IF NOT EXISTS 不会自动升级结构，需要先比较结构。不要同时并发运行初始导入。

## 数据范围

| 来源 | 分类代码 | 场所记录 |
|---|---|---:|
| 2608大厂养老院重点监管企业底数清单.xlsx | elderly | 11 |
| 高层住宅重点监管单位汇总清单.xls | highrise | 31 |
| 工贸企业重点监管单位汇总清单(3).xls | industry | 52 |
| 商业综合体重点企业汇总清单.xls | commercial | 7 |
| 合计 | | 101 |

已扫描项目中的 Excel/CSV，当前仅发现以上四份 Excel，没有 CSV；未包含不存在的文件。

- fire_place：101 条场所，类别专属字段采用可空列。
- fire_place_contact：202 条联系人角色关系，法人、主要负责人和现场负责人分别保存。相同姓名也保留不同角色。
- fire_place_import_row：101 条来源记录，保留文件哈希、工作表、原始行号、标题和所有列值；重复的“联系电话”表头按列号区分。
- 在 sys_dict_type/sys_dict_data 中补充 3 种业务字典、11 个字典项。已有同类型/同值字典不覆盖。
- status 使用若依已有的 sys_normal_disable 字典；del_flag 使用若依 0/2 约定。新增表复用 create_by/create_time/update_by/update_time/remark 风格。

空白和“/”不会转成零；原值完整保留。Excel 数字形式的联系电话转换成无 .0 的文本。23668.89 ㎡ 转换为面积数值 23668.89，其余无单位的建筑面积按平方米暂存，单位仍需业务核实。客流量保留文本，不擅自解释为日均值。

## 复用若依结构

fire_place.dept_id 逻辑关联 sys_dept，表示监管部门；supervisor_user_id 和 location_checked_by 逻辑关联 sys_user。上传数据不包含这些归属，全部暂留 NULL，不关联演示部门、不虚构人员账号。

场所联系人不是登录用户。小区是独立场所，物业公司写在 management_company，不按物业公司合并小区。

主键采用若依风格 place_id、contact_id、import_row_id，接口可映射为前端 id。部门/用户使用逻辑关联；联系人和来源记录对场所使用外键，无级联删除。

## 重复执行

来源键为“文件内容哈希 + 工作表 + 原始行号”的 SHA256。重复运行相同脚本，不增加重复场所、联系人和来源，也不覆盖已有人工修改。逻辑删除的数据不会被重新激活。

文件内容变化后会产生新的来源键，需核对是否为同一场所再导入。不能把更新后的文件直接当作自动更新脚本。

## 地图接入

四份清单没有坐标，longitude/latitude 全部 NULL，location_status 为 pending。脚本不调用高德服务、不生成猜测坐标。后续地址解析、点位核验后再标记 confirmed 并上图。

前端需要从真实接口读取场所，现有模拟模式不会自动使用数据库。接口字段映射：place_id -> id，longitude -> lng，latitude -> lat，coordinate_system -> crs。当前前端使用中文分类，需要将 industry/highrise/commercial/elderly 映射成重点企业/高层建筑/综合商业体/养老院，或统一改为代码筛选。按类别在前端选择图标。

## 校验结果

在独立临时数据目录、随机本地端口的 MySQL 8.0.27 中使用用户提供的真实字典表结构试跑，未连接或修改现有业务数据库。

- 首次导入：101 场所、202 联系人、101 来源。
- 101 条原始来源行的全部单元格、表头及标题逐项回读，与 Excel 一致。
- 无孤立联系人、无丢失来源关联、无伪造坐标。
- 在 STRICT_TRANS_TABLES,NO_BACKSLASH_ESCAPES 模式下重复执行通过；数量不增加，人工修改保留。

具体见 fire_place_import_report.json 和 fire_place_validation.json。数据 SQL 使用 UTF-8 十六进制字面量，避免引号、换行、反斜杠和客户端转义模式改变原始值。

SQL/tools 下提供生成及独立校验脚本，用于维护和复现；日常导入只执行 SQL 文件即可。生成器遇到新增 CSV 会停止，要求先审查字段映射，不会静默遗漏。