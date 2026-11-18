# 重点场所增删改查验证（2026-11-14）

## 交付范围

- `/fire/places` 的 PlaceManager / PlaceEditor 支持真实新增、详情、编辑、删除，以及名称/类别查询、URL 状态恢复和分页。
- 可维护基础资料、多个联系人和高德 GCJ-02 坐标。保存先读取完整联系人和版本；联系人编号保留，移除联系人及场所采用软删除。原始导入审计、装置及历史关联不删除。
- 独立新增/编辑/删除权限，编辑和删除分别按对应角色的数据范围检查。事务、行锁、版本冲突和共享去重锁防止越权及覆盖。
- 已有导入、详情拨号和消防主题继续复用。桌面名称列固定，窄屏可横向查看表格；表单内部滚动、底部按钮保持可达。表单重新打开清除旧校验状态。

## 本机数据库及运行状态

已执行 `python tools/place-management/migrate.py --apply`。升级前压缩备份路径见 migration.json，备份已排除 Git。337 条场所、437 条联系人及原有角色授权经过完整内容核对保持不变，仅新增版本字段、去重锁和缺失权限定义。未创建业务测试场所。

现有本地后端实例 PID 18196 已加载最新接口，OpenAPI 实际包含 POST /fire/places、PUT/DELETE /fire/places/{placeId}、GET /fire/places/{placeId}/edit。未另行启动第二个后端。普通角色需按工作职责分配新增/编辑/删除权限，并重新登录刷新会话。

## 已通过

| 验证 | 结果 |
|---|---|
| Maven 离线后端编译 | 通过 |
| tools/place-management/verify_project.py | 隔离 MySQL 30 项检查，真实事务、角色 DataScope、联系人稳定编号、冲突、回滚、软删除和历史保留；旧库增量升级重复执行 |
| tools/place-import/verify_project.py | 原始导入回归 30 项通过，235 条新增 / 90 条重复样本 |
| tools/device-capture/verify_project.py | 接入 652 项、装置管理 45 项、SSE 生命周期 12 项通过 |
| npm run test:fire | 67 项通过 |
| npm run build:prod | 生产构建通过，dist 已更新 |
| place-management.browser.cjs | 完整 CRUD、详情刷新、保存/加载失败、立即重试、冲突、草稿放弃确认、坐标验证、查询恢复、末页删除、只读角色、390px 表单通过 |
| place-import.browser.cjs | 原有导入预检/重试/结果/权限回归通过 |
| place-contacts.browser.cjs | 原有多联系人/拨号/错误恢复/过期响应/窄屏回归通过 |
| 前端设计严格审计 | 0 项问题 |
| DESIGN.md 官方 lint | 0 错误，1 条既有格式警告：项目维护的是 Markdown 设计上下文，没有 YAML token frontmatter；本次未新增/修改主题 token |
| git diff --check | 通过 |

浏览器验证使用真实 Vue/Element Plus 与受控 HTTP；数据库功能测试使用隔离 MySQL。仅增量升级在本机业务数据库实际执行。

## 设计契约核对

| 规则 | 证据 | 处理 |
|---|---|---|
| 场所此前只读 | 用户明确要求补齐 CRUD；现有后端原仅有查询 | 更新 DESIGN.md/UX-CONTRACT.md，新增真实服务和专用编辑器 |
| 不写演示副本 | places.js / FirePlaceWriteService / 传输回归测试 | 所有模式均使用真实接口，失败直接反馈 |
| 既有海军蓝/青色和 Element Plus | PlaceManager / PlaceEditor / fire.scss | 复用现有 token 与控件，不引入新主题 |
| 保留历史和来源 | 事务集成测试、软删除及联系人编号检查 | 无硬删除、不重建已有联系人、不改写原始 Excel 数据 |
