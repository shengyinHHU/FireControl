# 首页视频目录树验证（2026-11-15）

用户要求：首页视频监控使用类似“全部监控”的目录树，移除用户看不懂的设备编号和“状态未知”。

实现：dashboard 给现有 VideoMonitor 启用 compact + directoryTree 变体。使用同一个 Element Plus ElTree、videoRegions/videoDevices 和 LiveVideoDialog，单列展示文件夹与摄像头名称按钮。目录按需展开，摄像头点击/Enter 打开原播放器，各级目录通过自己的 lastId 加载更多。全部视频表示原有聚合范围，不推断设备所属机构。编号和原始/未知状态不出现在首页节点里；完整监控和查岗摄像头选择器保持各自原有布局。无数据库、权限或业务接口修改。

树节点复用现有深蓝/青色主题，选中和键盘焦点保持深色。长名称换行，目录树拥有单一内部滚动区；顶部刷新与底部提示保持可达。刷新使未完成的目录/分页请求失效；分页失败保留继续加载按钮，成功保留已展开子目录并去重，结束后移除继续加载按钮。空目录、加载失败和重试均展示真实反馈，不回退演示数据。

## 执行结果

在 RuoYi-Vue3 执行：

- `npm run test:fire`：71/71 通过，日志 unit-tests.log。
- `npm run build:prod`：最终代码通过，32.66s，日志 build.log。
- `node tests/fire/video-monitor.browser.cjs`：通过。新增验证首页目录逐级展开、摄像头名/隐藏编号和未知状态、键盘打开原播放器、分页失败重试且保留已展开目录、刷新忽略慢响应、空目录、目录失败重试、深色选中、长名称换行、390px 窄屏滚动和底部可达。原有完整监控目录、游标分页、授权错误、FLV 重连/清理、路由和权限检查同时通过。结构化结果 RuoYi-Vue3/docs/verification/video-monitor.json。
- `node tests/fire/dashboard-layout.browser.cjs`：通过。验证树区域随相邻面板拖动增加 120px，以及布局记忆/取消/键盘/重置/全屏/分辨率/窄屏行为。

浏览器使用本机无头 Chrome/Edge 和受控 HTTP/媒体响应（非现场设备流）；Playwright 依赖来自当前 Codex runtime。视频目录测试没有制造成功播放，保留授权失败反馈及已有受控媒体清理验证。目录展开/分页不自动申请视频流。

已查看桌面和窄屏截图：RuoYi-Vue3/docs/verification/video-monitor-dashboard.png、video-monitor-dashboard-narrow.png。截图显示受控测试目录和长名称，不代表现场目录内容。

根目录 premium 严格静态审计为 0 errors / 0 warnings（premium-audit.json）。官方 DESIGN.md lint 为 0 errors，保留既有“无 YAML frontmatter”提示。变更文件 git diff --check 通过。DESIGN.md 和 UX-CONTRACT.md 已记录 compact 目录树变体及现有组件归属；没有引入新的主题 token。
