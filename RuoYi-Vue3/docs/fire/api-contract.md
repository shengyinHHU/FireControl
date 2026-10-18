# 消防前端接口契约

这是前端约定，后端尚未实现；以此对接可保持页面不变。所有请求真实模式走若依Axios封装，路径前自动加 VITE_APP_BASE_API。私有接口必须验证登录、按钮权限、数据范围；前端按钮隐藏不能替代服务端授权。

## 通用格式
列表：`{ "code":200, "msg":"查询成功", "rows":[], "total":0 }`。
详情/写入：`{ "code":200, "msg":"操作成功", "data":{} }`。删除和标记已读返回data:null。
错误：使用若依code/msg；401登录失效、403无权限、409状态冲突/重复、业务校验返回可读msg。并发写入和短信去重必须由服务端事务/唯一约束保证。
ID全部为字符串。字段类型和完整样例见 entity-examples.json，派生字段列表响应应补充 placeName、gridName、personnelName。场所详情附 onDuty 数组，按当前时刻计算，不能仅按是否上报计算。
列表参数 pageNum(从1)、pageSize、keyword、placeId、gridId、category、status、source、result、dateFrom、dateTo；空参数不筛选，多个筛选AND。日期区间包含结束日全天。列表按业务时间倒序或稳定ID排序，不可分页抖动。

## 标准资源
places 场所、grids 网格、personnel 人员、materials 物资、hazards 安全生产、hosts 主机、devices 设备、cameras 摄像头、tasks 任务、inspections 查岗、alarms 警情、duty 值班表、messages 消息、smsRules 短信规则、sms 短信记录。

| 操作 | HTTP | 路径 | 请求/响应 |
|---|---|---|---|
| 分页列表 | GET | /fire/{resource}/list | query→rows,total |
| 详情 | GET | /fire/{resource}/{id} | →data完整记录 |
| 新增 | POST | /fire/{resource} | JSON字段，不带id→新增记录 |
| 编辑 | PUT | /fire/{resource} | JSON包含id→更新后记录 |
| 删除 | DELETE | /fire/{resource}/{id} | →data:null |

只读资源不开放任意CRUD：alarms通过同步创建、transition处置；messages通过事件创建、read动作已读；sms通过发送任务创建；duty通过授权H5提交；inspections从视频查岗创建。

## 领域动作
| HTTP | 路径 | 请求体 | data响应 |
|---|---|---|---|
| GET | /fire/dashboard | 无 | total,categories:[{name,count}],pending,processing,closed,hosts,online,cameras,unread |
| POST | /fire/alarms/{id}/transition | {action:"accept"或"close",note:"说明"} | 更新后警情，包括timeline |
| POST | /fire/devices/{id}/bind-camera | {cameraId} | 更新设备；设备PUT也可修改cameraId |
| POST | /fire/cameras/{id}/preview | {} | {mock:false,url,expiresAt,protocol} |
| POST | /fire/cameras/{id}/talk | {} | {mock:false,talkUrl,sessionId,protocol,expiresAt} |
| POST | /fire/personnel/{id}/call | {} | {mock:false,message,sessionId,url} |
| POST | /fire/grids/{id}/call | {} | VOLTE服务接受结果，不能未接通即返回已接通 |
| POST | /fire/messages/{id}/read | {} | null |
| POST | /fire/shares/_/create | {kind:"duty"或"video",resourceId} | {token,kind,resourceId,expiresAt,url?} |

报警状态仅允许 待处理→处理中→已办结，接单/办结说明必填，服务端写入操作者身份和服务器时间，不信任客户端传入的operator。已办结记录不可再次接单。
短信规则包括 name、source、trigger（接单/办结）、recipients（逗号分隔手机号）、enabled(boolean)。规则匹配时后端提交短信任务，记录真实发送结果；模拟仓库仅记录“模拟发送”。同警情同状态同规则不能重复发送。
截图 evidence 当前为JPEG Data URL；正式环境建议后端接收并转存文件后返回有权限的图片URL，同时限制体积、类型、读权限。不能将截图静默丢弃。

## 公开授权接口（仅以下接口可免登录）
| HTTP | 路径 | 参数 | 结果 |
|---|---|---|---|
| GET | /fire/public/shares/{token} | query:kind | data:{kind,resourceId,name,expiresAt} |
| POST | /fire/public/duty | {token,name,phone,date,start,end,submissionId} | data:值班记录 |
| POST | /fire/public/video | {token} | data:{mock:false,url,expiresAt,protocol} |

后端生成高熵、短期、可撤销、限定资源及用途的token；公开接口每次校验有效期和kind。不能允许传入任意placeId/cameraId越权。二维码只包含token，不含手机号、直接视频地址或若依登录token。公开接口限流、防重放；值班submissionId幂等，同人同场所重叠班次拒绝重复。失效授权返回业务错误410，不应跳转若依登录页。
H5只提交姓名、手机号、日期、起止时间；下班时间早于上班时间时代表次日。服务端存储startsAt/endsAt，响应应为含偏移的ISO8601；时间展示字段 occurredAt/time/lastSeen/deadline 约定 yyyy-MM-dd HH:mm:ss，Asia/Shanghai。

## 字段与约束
- places：name、category、gridId必填；category=重点企业/高层建筑/综合商业体/养老院；risk=低/一般/较高。owner、phone、address、description、lng、lat、crs。
- grids：name、owner、phone、description；polygon为不重复的至少3点坐标数组[[lng,lat],...]，API存储不含重复闭合点，转换GeoJSON时在末尾补首点。
- personnel：name、placeId、phone、role。materials：name、placeId、category、unit、quantity（非负）。
- hazards：name、placeId、category（风险源/隐患）、level、status（待整改/整改中/已整改）、owner、source、deadline、description。
- cameras：name、placeId、category（街道高点/城中村/无人机/布控球）、status（在线/离线）、lng/lat/crs。播放地址由preview获取，不放进列表永久保存。
- hosts：name、placeId、serial、model、status、loops、voltage、lastSeen。devices：name、placeId、hostId、cameraId、category、status、value、unit、lng/lat/crs。
- tasks：name、placeId、personnelId、deadline、description、status（待执行/执行中/已完成）。
- inspections：name（查岗人展示名；正式身份由服务端绑定）、placeId、cameraId、result（在岗/脱岗）、note、evidence、time。
- alarms：id、code、name、source（119警情/消控室主机/AI监测预警）、placeId、deviceId、status、level、occurredAt、timeline:[{time,action,note,operator}]。
- duty：见公开接口，加id、startsAt、endsAt、status=已上报。messages：name、alarmId、placeId、source、status=未读/已读、time。
- 引用实体必须存在，设备/摄像头/人员/任务属于同场所；不能移动已被引用的实体从而产生跨场所关系。删除存在引用的场所/主机/摄像头/人员/网格应拒绝并提供可读说明。
- 坐标必须为有限数字，lng[-180,180]、lat[-90,90]，crs明确GCJ02。实际辖区边界由业务方提供，示意数据不能作为监管边界。

## 外部接入责任
119/181数据同步、厂商视频鉴权/流协议、喊话音频传输、VOLTE会话、短信通道、GIS密钥代理均由后端或厂商配合。前端已有服务调用/展示和地图适配，真实设备与外部平台联调未完成。


## 2026-10-18 火警分类与地图定位口径（替代此前主机来源统计）
报警实体必须提供 alarmType：FIRE=火警，FAULT=故障，WARNING=其他预警。source仅表示接入来源，与报警类型独立；不要将119来源全部等同火警。缺少分类不会进入紧急火警面板，后端必须明确分类。
大屏消防警情：只展示alarmType=FIRE且status为待处理/处理中的记录，来源标签统计同一集合。
大屏消控主机：统计全部来源、全部类型的报警（含火警）；历史列表展示最近24小时，汇总数字为全部记录。
地图：未办结火警常驻火焰标记，普通图层筛选不隐藏火警；办结后移除。GCJ-02坐标优先取报警lng/lat，其次同场所关联设备坐标，最后场所坐标；无有效坐标不伪造位置。示意地图和高德适配器共用该定位集合。
大屏每30秒拉取更新；若需要秒级报警到达，后端需提供WebSocket/SSE事件协议，再触发统一刷新入口。
