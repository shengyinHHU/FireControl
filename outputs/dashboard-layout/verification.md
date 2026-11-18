# 大屏面板高度调整验证

业务依据：2026-11-14 用户要求左右两列面板支持上下拖动，以便增加视频监控空间、减少重点场所空间。

## 实现范围

- `PanelStack.vue` 统一左右列的三个分隔条、指针捕获、取消/键盘调整、局部重置和版本化布局记忆；`panelLayout.js` 负责相邻高度交换、最小高度限制和比例适配。
- 桌面调整只改变相邻两块，整列高度保持不变。保留标题/工具栏，场所、视频、查岗和警情内容在自己的区域内滚动。
- 本地浏览器分别记忆左右列比例；刷新恢复，视口或全屏变化时适配可用高度；拖动未完成时取消不写入。损坏或被禁用的存储不会使布局失效。
- 表头“还原布局”重置两列，双击分隔条重置本列。Up/Down、Shift、Home/End 和 Escape 可调整/取消，分隔条具备名称、值、边界和焦点样式。
- 宽度不超过 1100px 时使用自然页面滚动，不显示分隔条，不覆盖桌面偏好。
- 地图独立堆叠，地图连接错误提示不会遮挡面板分隔条。没有修改业务 API、权限或数据库。

## 执行结果

工作目录 `D:/javaprogram/FireControl/RuoYi-Vue3`：

| 命令 | 结果 |
|---|---|
| `npm run test:fire` | 71/71 通过，含 4 项高度分配/边界/偏好校验测试和全部 Vue 模板编译 |
| `npm run build:prod` | 通过，最终代码构建成功；日志见 build.log |
| `node tests/fire/dashboard-layout.browser.cjs` | 通过，使用真实 Vue 页面、production 配置和受控 HTTP/SSE 数据 |
| `node tests/fire/alarm-refresh.browser.cjs` | 通过，SSE、轮询、失败恢复、去重及离开页面的清理均未回归 |

浏览器使用环境变量 `PLAYWRIGHT_PATH=C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright` 和本机无头 Edge。布局测试验证三处分隔条实际拖动、摄像头列表增加 120px、两列恢复、取消不保存、方向键/加速/极值、最小高度、重置、1440×1000、1920×1080、1440×720、全屏进入/退出、390×844、损坏/禁用存储以及两个监控标签内部滚动。没有触发摄像头预览请求，页面异常为 0。结构化结果见 browser.json。

根目录严格运行 premium `audit_project.py --mode strict`：0 errors / 0 warnings，见 premium-audit.json。官方 `designmd lint DESIGN.md`：0 errors，保留项目既有的“无 YAML frontmatter”提示；本次不修改主题 token。`git diff --check` 通过。

## 视觉检查与边界

已检查桌面、1920×1080、1440×720 和窄屏截图：分隔条清晰，内容/滚动区跟随高度，底部操作可达，面板保持原有深蓝/青色样式。截图为受控场所/警情/摄像头数据；地图 SDK 故意中断以验证失败提示的层级，未验证真实设备流、现场地图网络或物理触摸屏。

布局偏好存储在当前浏览器，跨浏览器/电脑不共享。布局测试不会写入业务数据。
