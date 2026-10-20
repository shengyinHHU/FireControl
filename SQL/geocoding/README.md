# 第一阶段：高德候选坐标与更新 SQL

本工具只请求高德并写本地文件，不连接数据库、不执行 SQL。当前已准备全部 101 家机构的名称、地址和 source_key。尚未联网解析，没有虚构任何坐标。

## 1. 准备 Web 服务类型 Key

此前地图使用的 Key 按 Web端（JS API）配置。这里调用 REST 接口，需要高德控制台中平台为“Web服务”的 Key；若现有 Key 为 JS API 类型，请另外创建 Web服务 Key。JS API 的安全密钥不作为本工具的 jscode/sig 使用。

程序不会自动读取前端 .env.local。启动时会隐藏输入 Key，不写入日志、SQL、CSV 或缓存。若高德控制台为此 Key 启用了数字签名校验，请改用无需签名的独立 Web 服务 Key；本工具不实现签名算法。也可通过允许当前出口 IP 的服务端白名单限制 Key。

可选持久配置：复制 config.example.json 为同目录 config.local.json，填写 web_service_key。该本地配置及输出目录已被 Git 忽略。也支持 AMAP_WEB_SERVICE_KEY 环境变量。

## 2. 小批量试跑

在项目根目录 PowerShell 终端运行：

```powershell
.\SQL\geocoding\run_geocoding.ps1
```

默认仅 3 家、总时限 60 秒。程序会提示输入 Web服务 Key。已找到本机现有的 Python 运行时，无需安装依赖；若换电脑，需 Python 3.9 以上。

如果遇到权限或 Key 平台不匹配（例如 10009），会立即停止，不会继续循环请求全部机构。网络异常最多重试 1 次。Ctrl+C 可停止并导出已完成结果。

## 3. 全部 101 家

试跑正常后运行：

```powershell
.\SQL\geocoding\run_geocoding.ps1 -Limit 101 -MaxSeconds 300
```

每次 HTTP 请求 socket 超时 8 秒，父进程硬超时不超过 10 秒；使用子进程边界避免 DNS/读取卡住。单进程顺序执行，默认每秒最多开始一次请求，总网络处理预算 300 秒。每次成功查询立即写缓存；达到预算后停止新请求并输出结果。进程启动及本地文件输出可能略有额外开销，并非绝对墙钟时限。

如果达到总时限，重复同一命令续跑，成功查询会复用缓存。无匹配的成功响应也会缓存；暂时网络失败不缓存，可以重试。新输入/地址会产生不同查询缓存。

只访问高德地址解析与 POI 搜索固定接口，不启动浏览器、不启动后台常驻任务。对高德仅发送名称、地址、城市和 Key，不发送联系人电话等信息。

## 4. 输出文件

都在本目录 output 下：

- update_coordinates.sql：候选更新 SQL，供审查后手工导入。
- candidates.csv：UTF-8 BOM 审查表，包含所有候选、原始地址、匹配地址、经纬度、匹配级别和选择原因。
- summary.json：完成数量、未解决数量及停止原因。
- cache.json：成功接口响应缓存，用于断点继续，不含请求 Key。

不会把“道路中心”“街道中心”自动当成机构位置。唯一且精细的南京地址候选可输出待核验更新；若地址解析不明确，再用机构原名搜索 POI。仅本市、名称规范化后完全一致且没有未读取后续页的唯一 POI 候选才建议选取。多候选、异地和低精度记录保留在 CSV 中，不自动更新。候选不保证实际门址正确，仍需核对匹配地址及地图位置。

## 5. place_id 不猜测

默认输入 places_input.json 来自已交付的导入 SQL，没有数据库真实自增 ID，因此生成的 SQL 通过唯一 source_key 查询真实 place_id：

```sql
SET @fire_place_id = (SELECT place_id FROM fire_place WHERE source_key = ... AND ...);
UPDATE fire_place SET longitude = ..., latitude = ..., location_status = 'review'
WHERE place_id = @fire_place_id AND ...;
```

既不会按 Excel 序号推算 ID，也不会用单位名称直接批量更新。名称、原地址、类别和来源键都有快照保护；如果已导入后修改了名称地址，默认 SQL 会跳过该条。

如果希望 SQL 直接写出数字 ID，或者数据库中的名称地址已修改，请手动运行 export_places.sql，将查询结果导出为带表头的 UTF-8 CSV，然后：

```powershell
.\SQL\geocoding\run_geocoding.ps1 -InputFile 'D:\你的目录\places_from_db.csv' -Limit 101 -MaxSeconds 300
```

必须保留 place_id 的原始整数字符串，不用 Excel 将大整数转换成浮点或科学计数法。CSV 可使用 UTF-8 BOM 或 GB18030。真实 ID 模式生成 WHERE place_id = 数字，同时保留来源键（如有）、名称和地址保护。

默认模式不会发现数据库中新增加的其他场所；需要全部当前记录时，请使用数据库导出模式。

## 6. 审查多个候选后重新生成

复制 candidates.csv 为 reviewed.csv，检查每家机构候选：use 填 yes 表示选择，清空表示不导入。每个机构最多选择一条。唯一建议候选默认已填 yes，均需审查。不要修改 record_key、candidate_id、经纬度或地址列；重生成只接受缓存中真实返回的候选，不从编辑后的坐标列拼 SQL。

```powershell
.\SQL\geocoding\run_geocoding.ps1 -ReviewCsv '.\SQL\geocoding\output\reviewed.csv' -Limit 101 -MaxSeconds 300
```

此模式强制离线，不需要 Key。若之前使用数据库导出的 CSV，需同时传入相同的 -InputFile。输出 update_coordinates_reviewed.sql 和 candidates_reviewed.csv，不覆盖人工编辑的 reviewed.csv。

如需人工拖动修正坐标，应在后续地图核验功能中处理；此阶段不根据编辑 CSV 中的任意坐标生成更新。

## 7. 导入条件

阅读 CSV 和 SQL 后，再在现有数据库连接中手工执行更新文件。脚本目标库 dachang，不创建或删除表。

仅更新同时满足以下条件的记录：名称/地址/类别/来源符合输入快照；正常且未删除；经纬度都为空；位置状态为 pending 或 failed；没有核验人员与核验时间。

导入结果一律为 review（待核验），坐标系 GCJ02；不写 confirmed，不覆盖已有坐标或人工定位，不伪造核验人员。重复执行不会重复覆盖。脚本末尾输出实际更新条数和候选条数；实际条数偏少时应核对快照和已有坐标，不删除保护条件强行覆盖。

正式大屏若仅展示 confirmed 点位，候选 SQL 导入后仍需下一阶段核验后才会显示。

## 离线验证

已运行 test_geocode_places.py：输入完整性、真实 ID 精度、模糊匹配不自动选择、缓存续跑、错误 Key 立即停止、子进程硬超时、重复重试上限和 SQL 防覆盖等测试。测试使用模拟 API 响应，未使用真实 Key、未联网定位、未连接数据库。

## 官方接口依据

- 地理编码：https://lbs.amap.com/api/webservice/guide/api/georegeo
- POI 搜索：https://lbs.amap.com/api/webservice/guide/api/search/
- 错误码及 Key 平台要求：https://lbs.amap.com/api/webservice/guide/tools/info