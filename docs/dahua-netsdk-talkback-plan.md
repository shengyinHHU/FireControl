# 大华摄像机对讲接入方案

状态：已按本方案实现前后端、Maven接线、配置与增量SQL（尚未导入业务数据库）。2026-11-10更新。本文保留设计依据；实际接口、使用步骤和验证边界以[dahua-netsdk-deployment.md](dahua-netsdk-deployment.md)为准。

## 已确认的依据

- Windows独立工具对10.5.90.150已由用户确认现场可对讲，成功模式为P：PCM16裸音频通过CLIENT_TalkSendDataByStream发送，bNeedHead=1由SDK加头。
- 摄像机在视频监控列表中的名称由用户最新确认为“山潘新苑”。此前回复的214不直接作为天翼云deviceCode使用。
- 最终部署Linux；新增官方包General_NetSDK_ChnEng_JAVA_Linux64_IS_V3.060.0000003.0.R.251127。实际库为x86-64，尚未部署实测。
- 接入前的视频弹窗LiveVideoDialog.vue共用VideoTalkback.vue，开始对讲走天翼云语音接口；本轮已将共用入口改为大华NetSDK。
- 弹窗中“开启声音/关闭声音”的Microphone图标实际控制直播音量；独立“开始对讲”也使用Microphone图标。接入改动应落在对讲入口，保留两者清楚的用途。

## 推荐架构

保持现有FLV视频播放；复用现有“开始对讲/结束对讲”控件与fire:video:talk权限。大华设备改走项目的SDK对讲会话，前端不访问摄像机SDK端口，不持有设备密码。

浏览器麦克风 → AudioWorklet采样及连续重采样 → 8kHz/16bit/单声道PCM → 同源WSS → 若依后端会话 → CLIENT_TalkSendDataByStream → 摄像机。

摄像机音频 → CLIENT_StartTalkByDataType回调 → 复制裸音频及编码信息 → 有界队列 → 同一WSS → 浏览器解码及AudioWorklet播放。

后端只处理传输及SDK，不调用CLIENT_RecordStart采服务器麦克风，不调用CLIENT_AudioDecEx在服务器放声音。用户在远程浏览器讲话和收听。后端不需要声卡或桌面环境。

下行首期支持实际验证过的G.711A，并兼容G.711U、PCM16；浏览器转换成PCM播放。出现未知编码应明确报错并停止，保留错误信息，不显示虚假的“已连接”。能力与实际回调编码分别检查，不能用视频编码配置代替对讲编码能力。

开始SDK实时下行收听后，临时静音FLV音轨，避免延迟音轨叠加；停止后恢复开始前的音量状态。FLV画面继续播放，可能比实时对讲声音慢，不承诺音画同步。

## 点击入口与生命周期

1. 用户点击开始对讲；检查设备是否配置SDK连接信息和当前用户权限；在点击手势中准备AudioContext并请求麦克风。
2. 用户允许后，后端创建绑定用户与摄像机的短期一次性WSS票据；校验来源，凭据与若依长期token不放入URL日志。
3. 浏览器连接同源WSS，在首条控制消息提交短期票据；未鉴权连接不接收音频。鉴权后原子占用设备、SDK登录及查能力，使用已验证P模式的PCM参数和直接连接模式开启对讲。
4. 后端明确返回ready后才持续上传麦克风音频。发送参数bNeedHead=1、PCM16、8000Hz；建议40ms一包640字节，连续重采样保留分数相位，避免48k/44.1k采样时钟误差。期望包长和节奏在联调中验证。
5. 状态区分授权中、连接中、可发送、异常、已结束；“SDK提交成功”不标为“现场已听到”。收发计数分别显示。
6. 停止、取消、关闭弹窗、切换摄像机、页面离开、麦克风断开或WSS断开，都释放本地采音及后端SDK会话；失败不自动重新开启麦克风。
7. 同设备只允许一名用户对讲，其他请求提示占用，不自动抢占。多实例部署初期限定单个SDK服务实例，之后再增加分布式设备锁和路由。

建议连接超时10秒，心跳5秒、15秒超时，单会话最长10分钟。队列以约200ms为上限，积压时停止并提示重试，避免旧语音长时间后才播出。SDK回调只复制内存并入队，不做网络写入或磁盘写入，按handle/session路由，不能使用官方demo的单个全局talkHandle管理所有摄像机。

## 后端分工与接口草案

- SdkRuntime：按进程初始化/清理SDK，按操作系统选择官方库，保留回调强引用。Linux部署必须x86-64、64位JDK；SDK禁用时不加载原生库，不影响其他功能启动。
- DahuaTalkService：登录、查能力、P模式发送、回调接收、超时、停止、登出。按设备独立会话。SDK清理在应用停止时统一执行。
- TalkSessionService：用户与设备权限、会话所有权、并发占用、票据、幂等停止；不同用户不能操作他人会话。
- TalkWebSocketHandler：控制消息与二进制音频分离，严格校验长度/频率和会话状态。原生SDK返回长度可能含新增头，不能用必须等于PCM裸字节数的判断误报失败。
- POST /fire/video/devices/{deviceCode}/talk-sessions：创建短期会话/票据；DELETE /fire/video/talk-sessions/{id}：所有者停止；WSS /fire/video/talk-stream：交换鉴权、控制和双向音频。最终接口可沿现有控制器组织，但不复用云厂家的语音协议。
- 不把现有Windows命令行类直接复制成Spring服务，不启动Swing、不读服务器控制台、不使用Windows DPAPI。Windows与Linux使用同一业务接口，原生库按平台选择。
- 本阶段建议在ruoyi-system提供SDK适配和服务、ruoyi-admin暴露REST/WSS。SDK进程级初始化和DevTools重启需要明确隔离，不能反复加载同一原生库；若后续出现原生库稳定性问题，再把SDK适配拆为独立进程。

## 设备连接信息

复用fire_video_camera作为视频台账。计划新增fire_video_talk_endpoint，按camera_id一对一关联，用于平台设备到专网SDK设备的映射，不修改天翼云同步字段。

字段建议：endpoint_id、camera_id（唯一）、provider（dahua_netsdk）、host、sdk_port（37777）、channel（0）、transfer_mode（direct）、credential_ref、enabled、若依创建/更新/备注字段。登录账号密码通过后端配置或密钥文件引用提供；不向普通视频列表或前端返回密码，不把同一台设备凭据硬套所有设备。

首台映射：“山潘新苑”实际camera_id/deviceCode → 10.5.90.150:37777。实施时通过现有台账核实并绑定；重名时需用户选择，不能凭名称模糊匹配自动向设备发声。联系人和机构不因对讲接入新增设计。

## 运行配置与网络

- 配置项：fire.video.dahua.enabled、native目录、可写提取目录、credential引用、会话超时及并发上限；正式配置不携带本机加密文件。
- 浏览器访问HTTPS/WSS，localhost可本机调试。普通http://服务器IP不能正常采麦克风。Nginx配置WebSocket Upgrade和足够的空闲超时，同源代理若依鉴权。
- Linux服务器到摄像机的专网必须可达，37777端口至少需通；具体SDK后续数据连接端口以设备/厂家说明及部署抓包验证，不把37777可连视为全链路已通。
- Linux启动进程级设置LD_LIBRARY_PATH并用ldd检查依赖；不把厂商libssl/libcrypto覆盖到系统目录。首次部署检查uname -m（x86_64）、JDK、glibc、服务账号目录权限。
- 当前Windows电脑无法证明Linux .so加载与真实设备联调成功；部署测试由用户执行。若服务器是ARM64，则还缺匹配架构的官方SDK。

## 日志与验证

元数据日志默认开启：会话ID、操作者、摄像机标识、SDK接口/错误码、实际上下行编码、包数/字节、队列积压、停止原因和资源释放结果。不写密码、票据或完整鉴权地址。

复用独立工具双向WAV诊断的思路，但作为受控的调试开关，默认不长期录制业务音频；开启时按会话分开保存上下行、限制时长/磁盘并提供清理。上行录音代表浏览器采集并提交的数据，不冒充设备播放回执。

实施后的验证顺序：离线验证音频转换/分包、WSS鉴权与取消清理、多设备隔离；Windows验证Web页面经后端P模式对讲；Linux验证加载、路由、SDK登录及现场双向通话；最后核对关闭页面后设备可再次使用、原视频播放继续正常。现场人员明确确认出声才算最终验收。

## 本轮已经整理的文件

依赖在RuoYi-Vue-master/vendor/dahua-netsdk，含Java接口包、官方JNA5.13、Linux7个.so、Windows11个.dll、官方加载XML、两份README以及SHA-256清单。API源码比对3653份无差异；Java类依赖检查通过；Linux ELF架构检查通过。

已接入Maven/classpath及前后端业务接口，已生成SQL/fire_video_dahua_talk.sql。Windows原生库初始化和离线/浏览器测试通过；Linux与真实现场通话待部署验收。

## 实施时仍需确认/核实

1. “山潘新苑”在台账中的唯一camera_id/deviceCode（最新名称优先，214不盲用）。
2. Linux服务器CPU架构、发行版及版本，目标服务器到10.5.90.150的专网可达性。
3. 其他摄像机的IP、SDK端口与凭据，按台账逐台维护；首期只绑定这台已测试设备。

这些不影响当前方案和依赖准备，Linux最终兼容性与现场通话效果需部署后验证。
