# 天翼视联视频监控

## 入口与边界
首页查岗管理面板及 /fire/inspections 增加相邻的视频监控标签。原查岗数据、操作、记录保留。真实监控独立于演示 cameras 资源，目录不自动绑定业务场所，未知状态不伪装在线。

## 后端配置
RuoYi-Vue-master/ruoyi-admin/src/main/resources/application.yml 的 fire.video 已填写 AppID、AppSecret 和企业主账号。仅后台读取，禁止将该文件内容复制到前端。enabled 控制是否启用；allowed-user-ids 显式列出允许访问整个配置企业账号的普通用户。超级管理员默认允许。应用账号权限与运营商 Token 分开管理。

超级管理员无需额外导入菜单即可调用接口。普通用户先执行 SQL/fire_video_permissions.sql，授予 fire:video:list、fire:video:preview，并配置 allowed-user-ids；进入查岗页面仍需现有查岗查看权限。部署后重启后端加载配置，前端重新构建。服务器公网出口必须加入运营商白名单。

## 调用和播放
后台按运营商源码进行 XXTea 加密和 HMAC-SHA256 签名；缓存令牌，在返回有效期前 5 分钟重新获取。读取目录和设备后，播放接口只接受最近一小时在该配置企业账号列表中返回的设备编码。每次点击播放新申请 HTTPS HLS 地址并解密，地址不持久化。前端原生 HLS 或延迟加载 hls.js；关闭、切换标签或摄像头时销毁播放器。失败可手动重新连接，无无限循环重试。

设备接口使用 hasChildDevices=0，运营商直接返回可拉流的通道编码，不猜测主设备类型。按 lastId 游标加载更多；不伪造总数。目录 id 通过 cusRegionId 筛选，目录层级逐层读取。没有在线状态的返回显示状态未知。

## 验证
- Maven ruoyi-admin reactor package（跳过测试）验证所有后端模块编译。
- npm run build:prod、npm run test:fire。
- tests/fire/video-monitor.browser.cjs：实际 Vue UI，HTTP fixtures 验证分页、失败、重试、空列表、标签、窄屏和清理行为，不代表真实数据库联调。
- tools/video/verify_live.py：读取已配置凭据，直接调用新增 Java 服务，验证真实 Token、目录、设备和地址解密，以及账号访问拒绝。运行结果含临时授权 URL，runtime 已忽略。
- VIDEO_LIVE=1 运行浏览器测试：管理 API 仍由 Java 服务真实结果供给，媒体请求直接访问运营商；不是完整 Spring 登录/数据库端到端测试。

若清单或分片跨域受限，或设备编码不被浏览器支持，需要供应商开放跨域或额外媒体转码服务，不以接口 code=0 认定画面可播。查岗抓拍、喊话、回放和多画面均不在本次新增监控功能范围。

## 媒体跨域与编码实测补充
运营商 HLS 清单实测缺少 Access-Control-Allow-Origin。因此新增 FireVideoMedia：先通过登录及预览权限创建 10 分钟会话，再由相同用户携带若依 Authorization 请求清单与分片。媒体资源地址只由平台响应及清单解析得到，校验 HTTPS 和运营商域名；无任意 URL 代理、无匿名媒体接口。每个响应最多 16MiB，会话资源数量受限。

默认直播 mediaType=1，按本地旧版 cloud 直播示例表示标清；与 getVideoInfo 参数语义不同。IPCamera15 的高清及标清样本在本机无头 Edge 中均报告 hvc1（H.265）编码不支持，无法声明网页画面验收成功。HLS 清单和分片的真实后端转发测试通过。播放器在编码不支持时明确提示，并可下载新申请的 RTSP .m3u 给 VLC 播放；不会假装播放成功。要保证所有 H.265 设备跨浏览器网页播放，需要设备/供应商提供 H.264 码流或部署额外转码服务，本次未引入后台转码进程。

浏览器真实媒体模式实际读取 Java 服务刚下载的真实清单/分片样本，通过受鉴权路径的 HTTP fixtures 验证解码；不等同于持续实时流或完整 Spring HTTP 端到端测试。
