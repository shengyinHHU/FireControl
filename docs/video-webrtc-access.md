# 官方 WebRTC 播放接入（历史诊断）

2026-10-27 起，正式弹窗已改为 FLV，见 [FLV 接入说明](video-flv-access.md)。以下记录保留用于排查 WebRTC，不代表当前默认播放方式。

实时播放默认使用提供方 rtc_player 示例内的 SrsRtcPlayerAsync，替代原先默认 HLS/hls.js 播放。官方脚本随前端发布，无需另开示例网页。

后端复用现有账号鉴权、签名、令牌缓存、地址解密与设备范围校验，POST /open/token/vpaas/getDeviceMediaWebrtcUrl。业务参数为 accessToken、enterpriseUser、deviceCode、mediaType=1、netType=0、channel=0、mute=0。前端播放器初始静音，可在播放器内开启声音。

前端向原 /fire/video/devices/{code}/preview 请求 protocol=webrtc，将返回的 webrtc:// 或 rtc:// 地址交给官方 SDK。SDK仍使用提供方SDP协商流程，但通过同源 /fire/video/rtc/{session} 提交SDP，再由后端向 HTTPS /webrtc-gateway/rtc/v1/play/ 转发。真实网关未提供浏览器所需的跨域许可，直接访问会被CORS拦截。媒体仍通过WebRTC连接传输，不走原HLS分片代理。提供方签名播放地址只用于播放器，不在控制台打印。关闭、切换、重连释放 PeerConnection、媒体轨道和待处理信令。

原 HLS 显式协议调用及 RTSP/VLC 下载继续可用，播放失败不会静默改用 HLS。

部署：重启最新 ruoyi-admin，更新前端构建，保留 public/vendor/official-rtc 下所有脚本。无需修改数据库。浏览器须能访问返回地址指向的公网 RTC 节点、HTTPS 信令与媒体网络；页面登录令牌不发送给提供方 RTC 节点。

验证：后端离线编译、前端生产构建、本地浏览器模拟接口/SDP/连接生命周期检查。模拟检查不能证明真实摄像头已出画面；实际设备的地址响应结构、平台权限、信令跨域及公网媒体连通性需部署后验证。如提示未返回有效地址，提供脱敏响应结构即可进一步核对。

## 2026-10-26 实际诊断与修复

设备32011601021322001599取流地址获取正常。修复前浏览器报CORS错误，协商、ICE均未开始。修复后用实际Java转发方法取得网关SDP响应，ICE/DTLS连接成功，收到音频，但45秒内仅收到1个视频RTP包（90字节），framesReceived/framesDecoded均为0，浏览器请求关键帧23次。协商编码H.264。这个结果证明信令跨域问题已修复，但当时该次真实取流仍未出画面，不能将connected视为播放成功。

后端仅接收SDP，播放地址来自已鉴权设备的服务端取流结果；会话10分钟有效且绑定用户；只允许已观测官方网关vnet-voice-gateway.ctseelink.cn的HTTPS443地址，禁止重定向、任意客户端上游地址，限制请求与响应大小。原登录JWT不转发到提供方。

播放器45秒未出画面时报告实际收包/解码情况，释放连接并允许重连。诊断工具tools/video/verify_rtc.py及tests/fire/video-rtc-live.cjs有时间限制，不会启动长期设备采集。实际signed URL、SDP与凭据只保存在忽略的tools/video/runtime，禁止提交。

## 后续编码对照结果

同一设备新获取的HLS片段544824字节，MPEG-TS PMT视频stream_type=0x24（HEVC/H.265），音频0x0f（AAC）。本机Edge140.0.3485.94的RTCRtpReceiver能力列表未包含H.265，实际RTC offer/answer协商H.264。官方原版SDK直接播放与项目代理播放都未收到完整视频帧。新地址即时播放、高清/标清以及官方默认静音参数均未改善。

官方SDK解析前后完整streamurl保持一致，所有鉴权query参数逐项一致，未发现解密或参数截断问题。隔离的H.265-only合成offer探测被网关以业务code400拒绝，此单独探测不能证明网关完全不支持H.265。

当前最强线索是H.265源流与实测H.264 RTC链路之间缺少有效兼容/转码。下一步应以设备H.264码流或平台明确提供的H.264 RTC转码流做对照；不应仅凭code=0或connected宣布可播放。此轮只加强诊断工具与报告，没有改动正式播放器或切回HLS。

## Chrome原生H.265协商实测

Chrome155.0.8059.86实际能力包含H.265。通过原生setCodecPreferences仅提供浏览器实际支持的H.265，官方原版SDK使用新获取地址直接调用官方网关：HTTP200，业务code400，协商未建立，未收到answer。此结果与前次合成SDP探测分开：本次是Chrome生成的真实有效H.265 offer。当前节点/请求不能强制H.265起播，需提供方确认H.265 RTC支持条件，或提供H.264转码流；不要把浏览器版本本身认定为根因。__MACOSX目录10个文件均为AppleDouble元数据，无额外SDK或解密实现。未将这个已失败的强制模式写入正式默认播放流程。

## Chrome H.264-only对照

按用户要求使用Chrome155原生setCodecPreferences只提供H.264，官方原版SDK及即时新取流地址：网关接受并回答H.264，ICE/连接为connected，但测试期间仅1个视频包90字节、framesReceived/framesDecoded=0，画面宽度0。因此约束浏览器协商为H.264没有使这台摄像头出画面；还需验证源设备H.264输出或平台有效转码。此次只更新诊断脚本与证据，未修改摄像头或默认正式播放行为。
