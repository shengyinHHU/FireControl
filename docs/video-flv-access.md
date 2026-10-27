# FLV 直播接入

大屏和管理页共用 LiveVideoDialog，默认只请求 FLV，不自动切回 HLS 或 WebRTC。

## 接口与播放

后端通过 `/open/token/cloud/getDeviceMediaUrlFlv` 获取直播地址，沿用现有 accessToken 缓存、enterpriseUser、公共参数签名和 version=v1.0 的 XXTea 解密。业务参数为 deviceCode、mediaType=1、mute=0（保留音轨）。国标/社标设备由平台返回高清流。

浏览器只收到 `/fire/video/flv/{session}`，携带当前登录令牌读取视频。后端验证权限和会话归属，仅访问允许的 HTTPS 媒体节点；禁止重定向，不接受浏览器传入上游 URL，不转发用户 JWT 到厂家。取流地址不写入日志。FLV 使用连续转发和逐块刷新，不等视频结束再返回。

FLV 会话创建后 10 分钟内可首次连接，且只能使用一次。连接后可持续观看；上游连续 20 秒无数据会超时。重连重新申请地址，关闭弹窗或切换设备销毁播放器、停止下载，并调用 DELETE 关闭后端连接。浏览器异常退出时，断开的输出连接或上游空闲超时负责回收。

浏览器初始静音以允许自动播放；统一工具栏声音按钮负责开关声音。初始静音不等于向平台请求去除音轨。

前端使用 mpegts.js 1.8.0 按需加载，将 FLV 转为浏览器可播放的媒体片段。支持播放器所支持的 H.264/H.265 封装，但 H.265 仍依赖浏览器与操作系统解码能力，程序不做转码。只有实际出现视频画面才显示“播放中”，异常、无画面超时、授权失效都显示提示。保留手动重新连接、全屏和显式 RTSP/VLC 下载。

## 本地运行

前端目录安装依赖 `npm install`，再启动真实后端模式 `npm run dev:real`；已有开发进程需要重新启动。IDE 重新运行最新 ruoyi-admin，刷新浏览器后从视频监控点击摄像头。无需新建数据库表，也无需新增厂家凭证配置。

正式部署更新前端构建产物及后端。若使用 Nginx，FLV 对应代理路径应关闭 proxy_buffering、proxy_cache 和 gzip，并设置足够的 proxy_read_timeout，例如 60s；仍转发原 Authorization。后端已返回 X-Accel-Buffering:no 和 Cache-Control:no-store。每路持续视频占用一个 Servlet 请求线程和一条上游连接，后续大量并发需评估线程、带宽和媒体服务容量。

FLV 是观看通道。本次不包含语音对讲；后续须另接厂家的对讲接口或 SDK。

## 验证

`tools/video/VideoFlvCheck.java` 使用现有配置，限量采样并验证用户隔离、会话防重放；serve 模式只在 127.0.0.1:5186 建立隔离验证端点，60 秒自动停止，不连接业务数据库，不用于部署。验证运行时文件不纳入 Git。

`RuoYi-Vue3/tests/fire/video-monitor.browser.cjs` 默认使用受控 HTTP 验证列表、授权失败、重连、关闭和释放连接；VIDEO_LIVE=1 使用本地厂家 FLV 样本解码；VIDEO_FLV_RELAY=1 经隔离服务调用实际生产控制器、取流服务和厂家流，验证 Chrome 持续解码。隔离测试的目录和登录权限为夹具，不能替代完整部署的权限联调。

实测摄像头 32011601021322001599，厂家 FLV 数据约 3.5 MB，Chrome 实时解码尺寸 1920×1080，播放时间推进至 5.03 秒，累计 130 帧。详见前端 docs/verification/video-monitor-flv-live.json 与 video-flv-live.png。编译、构建和浏览器检查通过不代表所有设备或所有浏览器均可解码。

## 铭望府 G.711 兼容修复

设备 32011601031322001324 的实际 FLV 样本约 3.21 MB：视频 codecId=12（HEVC），298 个音频标签 soundFormat=7（G.711 A-law）。源文件头遗漏 audio flag，仍含有效音频标签。mpegts.js 1.8.0 的常规 FLV 音频解析只接受 MP3/AAC，因此原提示“视频解码失败”实际上由不支持的音轨触发。[播放器源代码](https://github.com/xqq/mpegts.js/blob/v1.8.0/src/demux/flv-demuxer.js)

现在仅在明确检测到 G.711 编码错误时重连 FLV，由 mpegts.js 解码视频，独立 Web Audio 解码 G.711（8000 Hz）并跟随视频播放时间；AAC 路径不变。所有编码统一使用“开启声音/关闭声音”按钮，隐藏浏览器原生音量控件；无音轨不显示声音按钮。不开麦克风，不依赖外部程序。队列最多 400 个音频包，暂停/等待停止已调度音频，关闭回收 AudioContext 和流。

样本和后端实时转发已在 Chrome 155 验证 2560×1440 画面（实时 130 帧，时间推进 5.03 秒）及非零 PCM 播放，按钮开关生效。报告：RuoYi-Vue3/docs/verification/video-monitor-flv-g711.json。此项验证证明音频解码和输出图正常，无法替代本机扬声器及现场收音检查。G.711 编码参考 [RFC 3551](https://www.rfc-editor.org/rfc/rfc3551#section-4.5.14)。
