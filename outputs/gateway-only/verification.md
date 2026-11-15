# 传输装置管理精简验证（2026-11-14）

## 结果

仅管理传输装置。移除全部下挂主机/装置详情主机页面、主机表单、接口、服务、Mapper、自动建档和主机统计。系统/部件原始协议对象仍直接保存为传输装置事件，保留地址、原始内容、去重和提交后确认逻辑。

本机 dachang 已备份后执行 SQL/fire_gateway_only_upgrade.sql，旧后端停止后升级，并以原启动参数、工作目录和环境重启。业务库核对：37 台传输装置、106 条历史事件、980 条通信报文保持不变；37 条主机记录及 fire_host、fire_event.host_id 已删除；105 条菜单和 86 条角色关联保留，角色权限迁移前后等价。详情见 ../device-diagnostics/gateway-only-migration.json。压缩备份在该报告记录的路径，已排除 Git。

运行中的 OpenAPI 已核验 /fire/gateways/stats 存在，/fire/hosts* 及 /fire/assets/hosts* 均不存在。HTTP 8080 和消防 TCP 2000 已恢复监听。

## 通过

- `python tools/device-capture/verify_project.py`：隔离 MySQL，接入 652 项、装置管理 45 项、SSE 生命周期 12 项检查；另验证旧表/旧权限迁移、角色关联、完整事件/原文保存、重复执行及重新导入不恢复主机表。
- `mvn -o -pl ruoyi-admin -am -DskipTests compile -q`：后端编译。
- `npm run test:fire`：67 项通过。
- `npm run build:prod`：生产构建通过，dist 已更新。
- `node tests/fire/device-access.browser.cjs`：真实 Vue + 受控 HTTP，装置增删改查、失败草稿、无主机入口/API、事件/报文详情、空状态/重试、390px 表单、只读角色、大屏统计与旧书签。
- `node tests/fire/incident119.browser.cjs`：119 来源及筛选、详情、错误恢复、同级装置页面与大屏回归。
- `node tests/fire/alarm-refresh.browser.cjs`：告警 SSE/轮询刷新、失败恢复和卸载清理。
- `node tests/fire/home-routing.browser.cjs`：生产模式登录/首页和真实后台 GET 路由一致性。
- 前端设计严格审计：0 项问题；`git diff --check` 通过。

浏览器验证使用受控接口，不等同于真实消防装置现场联调。业务库升级及本地后端启动已实际执行。

## 既有回归失败

额外运行 `python tools/119-secret-key/verify_project.py`，在 Fire119IntegrationTest.java:180 的 `geometry OR address/description keyword includes outside and missing coordinates` 断言失败。另将修改前 HEAD 源码导出到独立临时目录，使用其原 SQL、Java 和 Mapper 编译复测，同一断言失败，已确认不是本次删除主机功能引入。基线日志：119-head-baseline.log。本次未改动 119 地理筛选业务逻辑。

普通账号如缓存旧的 fire:hosts:* 权限，需要重新登录刷新会话；现有角色授权已迁移为对应 fire:gateways:*。
