# 摄像头地图图标验证（2026-11-14）

用户要求：摄像头图层使用图标，放大后显示名称，点击保留原来的弹窗功能。

## 实现

- mapAdapter 使用 mapPresentation.js 的静态摄像头 SVG，复用 mapPresentation.css 中的资源点位框、标签、悬停和焦点样式。
- 摄像头固定为 38×38px，锚点位于原坐标中心。AMap zoom >=17 时显示名称，缩小后隐藏；悬停/键盘聚焦可临时查看。标签宽度上限 190px，长名称省略，完整名称保留在 title、可访问名称和详情中。
- 名称通过 textContent 写入，SVG 是代码内固定图形。保留原生按钮和已有 onSelect/selectCamera、监控点位弹窗及 查看视频/LiveVideoDialog 流程。
- 不改变业务数据、坐标来源、账号/权限、图层过滤、加载/错误反馈或播放接口；没有增加自动播放请求。
- 资源点位公共颜色变量改名为 marker-color/marker-bg 以支持场所和摄像头共同使用；场所类别原有颜色/轮廓值保持不变。摄像头沿用原来的薄荷绿色类别颜色。

## 验证结果

在 RuoYi-Vue3 目录执行：

| 命令 | 结果 |
|---|---|
| `npm run test:fire` | 71/71 通过，包含地图点位生命周期、场所聚合、权限/数据源约束和全部 Vue 模板编译 |
| `npm run build:prod` | 通过，50.96s，见 build.log |
| `node tests/fire/camera-map.browser.cjs` | 通过，production 模式真实 Vue/adapter DOM，HTTP 和地图 SDK 为受控夹具 |
| `node tests/fire/video-coordinates.browser.cjs` | 通过，原有坐标维护、导入/编辑、地图详情和共享播放弹窗流程均未回归 |

Playwright 使用当前运行时依赖目录及本机无头 Chrome。新浏览器测试覆盖：加载提示；低缩放图标和隐藏名称；不改坐标/排除无坐标设备；38px 固定尺寸；悬停、实际 Tab 焦点及 Enter 打开详情；17 级显示名称/长名称省略；缩小隐藏；点击打开原弹窗；只有显式 查看视频 触发预览；原播放器授权失败反馈；重复开关图层/搜索无残留；390×844 下弹窗在视口内；错误、重试和空目录。页面异常为 0，结构化结果见 browser.json。

视觉检查：overview.png、zoomed.png、narrow-dialog.png。窄屏弹窗等待进入动画结束后截取，完整名称和原有操作均可达。SDK 夹具不模拟真实底图/缩放投影，所以截图黑色底图和点位间距不代表现场地图；本次未发起真实摄像头播放或修改数据库。

根目录 premium 静态严格审计：0 errors / 0 warnings，见 premium-audit.json。官方 DESIGN.md lint：0 errors，只有项目原有“无 YAML frontmatter”提示。变更文件 `git diff --check` 通过。
