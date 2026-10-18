# 消防菜单与权限对接

前端路由已由 src/router/modules/fire.js 注册。不要再由 getRouters 返回同名 /fire 页面，否则可能覆盖路由守卫。角色菜单返回保留原有若依系统管理菜单。生产消防菜单按用户权限过滤；每个消防页面还有 beforeEnter 检查。

menu.sql 仅添加一个隐藏权限目录及按钮权限节点，不授予任何角色、不改用户、不改原菜单。请后端在测试库审核后执行，再通过若依角色管理勾选对应权限。超级管理员沿用若依 *:*:*；前端不创建管理员账号。

|资源|入口/功能|权限|
|---|---|---|
|dashboard|指挥大屏|`fire:dashboard:list`|
|places|重点场所|`fire:places:list`, `fire:places:add`, `fire:places:edit`, `fire:places:remove`|
|grids|网格管理|`fire:grids:list`, `fire:grids:add`, `fire:grids:edit`, `fire:grids:remove`, `fire:grids:call`|
|cameras|视频监控|`fire:cameras:list`, `fire:cameras:add`, `fire:cameras:edit`, `fire:cameras:remove`, `fire:cameras:preview`, `fire:cameras:share`, `fire:cameras:talk`|
|inspections|查岗管理|`fire:inspections:list`, `fire:inspections:add`|
|alarms|警情处置|`fire:alarms:list`, `fire:alarms:edit`|
|hosts|消防主机|`fire:hosts:list`, `fire:hosts:add`, `fire:hosts:edit`, `fire:hosts:remove`|
|devices|设备台账|`fire:devices:list`, `fire:devices:add`, `fire:devices:edit`, `fire:devices:remove`|
|personnel|责任人员|`fire:personnel:list`, `fire:personnel:add`, `fire:personnel:edit`, `fire:personnel:remove`, `fire:personnel:call`|
|materials|消防物资|`fire:materials:list`, `fire:materials:add`, `fire:materials:edit`, `fire:materials:remove`|
|hazards|安全生产|`fire:hazards:list`, `fire:hazards:add`, `fire:hazards:edit`, `fire:hazards:remove`|
|tasks|任务派发|`fire:tasks:list`, `fire:tasks:add`, `fire:tasks:edit`, `fire:tasks:remove`|
|duty|值班上报|`fire:duty:list`, `fire:duty:share`|
|messages|消息中心|`fire:messages:list`, `fire:messages:edit`|
|smsRules|短信规则|`fire:smsRules:list`, `fire:smsRules:add`, `fire:smsRules:edit`, `fire:smsRules:remove`|
|sms|短信记录|`fire:sms:list`|

## 角色组合建议
- 大屏值守：dashboard、places、grids、alarms、hosts、devices、cameras、inspections 的 list；按需要添加人员、物资、隐患 list 及 cameras:preview。大屏需要这些数据源权限，避免后端拒绝其聚合查询。
- 警情处置：alarms:list/edit，加 places:list、devices:list、cameras:list/preview；摄像头授权分享另授 cameras:share。
- 场所维护：places:list/add/edit/remove、grids:list，及需维护的 personnel/materials/hazards 权限；场所详情中无权限的关联数据不请求。
- 设备维护：devices、hosts、cameras 的所需写权限与 places:list；编辑设备关联时需要 hosts:list/cameras:list。
- 值班管理：duty:list/share 和 places:list。H5使用短期分享token，不授予后台权限。
- 短信设置：smsRules:list/add/edit/remove；查看发送记录需要 sms:list。

系统管理沿用 system:user:*、system:role:*、system:dept:* 及个人中心；用户必须先登录真实后端。

正式服务端权限与数据范围必须独立检查。前端列表和按钮权限只改善用户体验，不能保护接口。