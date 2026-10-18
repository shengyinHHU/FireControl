# 大厂街道智慧消防监管前端

## 本地运行
项目根目录：`D:\Downloads\RuoYi-Vue3\RuoYi-Vue3`。

```powershell
cd D:\Downloads\RuoYi-Vue3\RuoYi-Vue3
yarn dev
```

启动后自动打开 http://localhost/fire/dashboard ，访问根路径或 /index 也会进入消防大屏。若80端口已占用，请使用终端显示的实际地址。模拟环境无需启动后端或登录。浏览器中的模拟修改持久保存；管理页顶部“恢复示例数据”可恢复初始数据。重新安装依赖可用 `yarn install --frozen-lockfile`（保留原工程Yarn锁文件）；新增唯一直接依赖为 qrcode@1.5.4。当前目录已安装所需依赖。

真实后端开发模式使用 `yarn dev:real`；生产构建 `npm run build:prod`，产物在 dist。模拟模式需要同时满足 Vite DEV 和 VITE_FIRE_MOCK=true；生产构建不会启用演示免登录入口，不会调用模拟仓库。

## 页面入口
- `/fire/dashboard` 指挥大屏；`/fire/places` 重点场所；`/fire/grids` 网格地图。
- `/fire/video` 视频资源；`/fire/inspections` 查岗；`/fire/alarms` 全来源警情处置。
- `/fire/hosts` 主机；`/fire/devices` 设备；`/fire/personnel` 人员；`/fire/materials` 物资；`/fire/hazards` 安全生产；`/fire/tasks` 任务。
- `/fire/duty` 值班表；`/fire/messages` 消息；`/fire/sms` 短信规则和发送记录。
- `/fire-public/duty?token=...`、`/fire-public/video?token=...` 为短期授权链接。缺少/过期token不可使用。二维码从场所值班页或设备视频生成。

## 推荐验收顺序
1. 大屏点击场所，查看人员/物资/安全生产；派发任务后在任务页查询。
2. 警情详情输入说明，接单再办结，检查时间线和大屏数字。
3. 视频页面打开在线摄像头、抓拍、填写查岗人、保存；查岗页查看图片证据。
4. 设备编辑选择同场所摄像头；从告警详情打开联动视频。
5. 网格页打开网格地图，绘制至少3点边界并保存；点击场所下钻。设备编辑可在地图上选取坐标。
6. 值班页生成二维码，打开H5，上报后回到值班页查询；大屏场所的当前在岗人员随班次更新。
7. 短信规则启用对应来源和“接单”条件，处置匹配警情后查看模拟发送记录。

## 接真实后端
保持页面不变，实现 `api-contract.md` 中的接口。`src/api/fire/index.js` 是页面唯一服务入口；真实请求沿用若依 src/utils/request.js。系统用户、角色、部门、超级管理员和个人中心继续使用原若依功能，需要后端登录和数据库。
生产管理页使用若依 Layout；独立大屏使用消防全屏布局。`menu-permissions.md` 说明静态消防菜单与后端权限的关系，避免重复注册相同路由。

## GIS配置
默认使用明确标注的示意底图，点位和网格也为演示坐标。配置实际地图时，在 `.env.development.local` 或部署环境中加入：

```dotenv
VITE_FIRE_MAP_PROVIDER=amap
VITE_FIRE_AMAP_KEY=你的Web端JSAPI的key
VITE_FIRE_AMAP_SERVICE_HOST=https://你的站点/_AMapService
VITE_FIRE_PUBLIC_ORIGIN=https://你的站点
```

`src/api/fire/mapAdapter.js` 提供高德JS API 2.0适配：点位、网格、图层、POI、实时路况、缩放与坐标转换。安全密钥保存在后端代理，不写入前端构建变量；代理配置参考高德官方文档：https://lbs.amap.com/api/javascript-api-v2/guide/abc/jscode 。实际地图需有效密钥、服务授权、网络及真实网格边界，当前未进行真实服务联调。
编辑点位保存 GCJ02 经纬度。其他坐标系入库前通过适配器 convert() 或后端进行转换；不要直接将WGS84/BD09坐标当GCJ02存储。

## 视频、短信、呼叫和外部数据
- 模拟视频由Canvas绘制，可抓拍存证；模拟喊话不会发送音频，模拟呼叫不会拨号。
- 真实预览接口返回短期授权且浏览器可播放的URL。若设备提供FLV/WebRTC/私有协议，请在 VideoDialog 的播放/语音适配位置接入厂商SDK；语音通道的网络传输尚需厂商协议，不会假报“已接通”。
- 119数据库同步/消息推送、181平台归集、短信发送由后端接入。前端已提供列表、分类、规则、状态与动作接口。
- 二维码为真实二维码。当前模拟授权只存在同源浏览器存储中，另一部手机不能读取桌面浏览器的模拟授权。跨设备使用需部署后端授权接口，并设置手机可达的域名；localhost只指当前设备。
- H5使用手填姓名/手机号；未创建微信小程序或实现微信手机号鉴权。

## 验证与维护
`npm run test:fire` 执行业务规则测试；`npm run build:prod` 验证生产构建。接口返回时间字符串约定Asia/Shanghai，授权到期时间为毫秒时间戳。模拟数据可能过期，跨日演示请恢复示例数据。
修改前原文件保存在 work/originals；此目录原本不是Git仓库，未创建提交。未修改后端源代码。

模拟底图已按参考图改为黑蓝路网、河流、青色地名与白色区域边界。路网、地名位置和默认区域轮廓仅用于视觉演示，不代表真实地理数据；编辑边界和高德地图使用实际存储坐标。默认隐藏摄像头/设备图层，可通过地图左侧按钮开启。

方案B已实施：桌面大屏为全幅地图和左右悬浮面板；资源图层、视频工具可折叠；点击火警显示右侧详情并定位地图，通过“查看与处置”进入原处置流程。低高度桌面压缩主机统计以保留报警列表，窄屏保留纵向布局。
