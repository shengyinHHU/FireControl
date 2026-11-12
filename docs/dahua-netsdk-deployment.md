# 大华摄像机对讲接入与部署

当前实现保留天翼云 FLV 画面，将共用视频弹窗的“开始对讲”接到项目后端的大华 NetSDK。摄像机侧使用已验证的 P 模式：PCM16、单声道、8000Hz，通过 `CLIENT_TalkSendDataByStream` 提交，`bNeedHead=1`。浏览器负责采音和收听；Linux 服务端不调用本机录音或音频播放接口。

## 1. 导入增量 SQL

将 `SQL/fire_video_dahua_talk.sql` 导入 `dachang`，已有 `fire_video_camera` 视频台账必须存在。新表 `fire_video_talk_endpoint` 一对一关联实际台账记录，保存专网 IP、SDK 端口、服务器登录配置引用、启用状态和版本；不保存密码。脚本可重复执行，不写入测试摄像机数据。

管理员默认具备权限；其他角色使用对讲需 `fire:video:talk`，维护连接另需 `fire:video:talk-config`。仍须满足现有 `fire.video.allowed-user-ids` 监控账号授权规则。新增权限后可重新登录刷新角色权限。

## 2. 设置服务器登录配置

`ruoyi-admin/src/main/resources/application.yml` 已加入 `fire.video.dahua`，默认 profile 名为 `default`。通过运行进程环境设置：

| 环境变量 | 含义 |
| --- | --- |
| `FIRE_DAHUA_PASSWORD` | 摄像机实际密码，必填，无默认值 |
| `FIRE_DAHUA_USER` | 摄像机账号，默认 admin |
| `FIRE_DAHUA_ORIGINS` | 网页来源白名单，逗号分隔，如 `https://fire.example.com`，不含路径、末尾斜杠 |
| `FIRE_DAHUA_EXTRACT_DIR` | SDK 可写提取目录，默认 `./runtime/dahua-sdk` |
| `FIRE_DAHUA_ENABLED` | 对讲功能开关，默认 true |

Windows 本机测试可在 IDE 的运行配置中设置这些环境变量；Linux 使用 systemd 的受限权限 EnvironmentFile 或部署平台密钥注入。不要把实际密码放到前端、URL 或提交到仓库。不同设备账号不同，可在外部 Spring YAML 中增加 `fire.video.dahua.credentials.<配置名>.username/password`，重启后在连接窗口选择对应配置。

改变 Maven 依赖后，IDE 需要重新加载 Maven 项目。SDK 使用进程原生库，联调建议用完整进程重启，避免 DevTools 热重启；启动脚本已关闭 DevTools restart。

## 3. 绑定真实摄像机

在“视频监控”中打开**山潘新苑对应的实际视频记录**，点击“连接配置”：

- 摄像机 IP：`10.5.90.150`。
- SDK 端口：`37777`，不是网页登录的 80。
- 登录配置：`default`；开启“启用对讲”，保存。

若提示未同步监控台账，先到视频台账同步设备，再重新打开连接配置。绑定依据是选中记录的实际 `deviceCode` 经账号范围查得的 `camera_id`。没有将 214 硬编码为天翼设备编码，也不按名称自动猜测绑定。后续摄像机逐台配置自己的地址与登录引用，同一 IP+SDK 端口不允许重复绑定。更改或解除绑定前需结束当前设备的对讲。

## 4. Linux x86-64 部署

当前提供的 `.so` 为 **x86-64**。先运行 `uname -m`，结果应为 `x86_64`；64 位 ARM 需要另外获取厂商匹配版本。使用64位 Java 17或更高版本，并检查目标发行版动态链接依赖。

必要依赖位于 `RuoYi-Vue-master/vendor/dahua-netsdk/`，已经接入 Maven 普通依赖及运行资源。运行包包含 Windows/Linux 原生库。部署还应将 `native/linux64` 整体复制到服务器，例如 `/opt/firecontrol/dahua/linux64`，用于动态库的间接依赖搜索：

```bash
ldd /opt/firecontrol/dahua/linux64/libdhnetsdk.so
ldd /opt/firecontrol/dahua/linux64/libdhconfigsdk.so
```

若显示 `not found`，先补齐对应发行版依赖；不要覆盖系统的 SSL 库。设置进程 `LD_LIBRARY_PATH=/opt/firecontrol/dahua/linux64`；给提取目录配置服务账号读写权限，摄像机专网地址应从该服务器可达。

已提供 `tools/dahua/start-linux.sh`，可通过 `bash` 执行。脚本读取 `FIRE_DAHUA_NATIVE_DIR` 和 `FIRE_APP_JAR`（均为绝对路径时可任意放置应用），设置 `LD_LIBRARY_PATH` 并启动进程。`FIRE_DAHUA_NATIVE_DIR` 是启动脚本变量，不是应用 YAML 属性。默认路径适用于完整仓库布局。无需服务器声卡或桌面环境。

构建命令：

```bash
cd RuoYi-Vue-master
mvn -pl ruoyi-admin -am clean package -DskipTests
```

Java 依赖使用 vendor 下的文件 Maven 仓库，不需要先手工 install SDK。项目已有 OSHI 使用 JNA 5.19.1，本实现与之统一为 `jna-jpms:5.19.1`，避免同时装载 JNA 两套同名类；厂商 JNA 5.13 文件留作原始配套资料，未重复打包。

## 5. HTTPS 与 WebSocket 代理

本机 `localhost` 可采麦克风；其他电脑访问服务器必须使用可信 HTTPS，普通 `http://服务器IP` 不满足采音条件。应部署可信证书或给客户端安装受信任的内网 CA，不使用浏览器不安全开关。设置 `FIRE_DAHUA_ORIGINS` 为实际网页来源。

在现有 Nginx 的 API 代理中加入 WebSocket 升级，例如前端 API 前缀为 `/prod-api/`：

```nginx
# http 块
map $http_upgrade $fire_connection_upgrade {
    default upgrade;
    '' close;
}
# 已配置 HTTPS 的 server 块
location /prod-api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $fire_connection_upgrade;
    proxy_set_header Host $host;
    proxy_read_timeout 60s;
    proxy_buffering off;
}
```

沿用实际部署已有 API 前缀，勿重复添加同名 location。前端 Vite `/dev-api` 代理已支持 WS。

REST 接口经过若依 JWT、视频权限及监控账号授权；WS 使用首条消息中的30秒一次性票据，原始 JWT 与密码不进入 WS URL。未鉴权连接5秒关闭。5秒心跳、15秒失联关闭、单次最长10分钟；默认最多4路，每个用户及摄像机各限一路。当前锁和会话位于单个后端进程，部署时应保持单实例；多实例需要增加分布式设备锁及会话路由后再启用。

## 6. 联调与诊断

打开视频并点击“开始对讲”，允许麦克风。成功进入“正在双向对讲”后再讲话，建议佩戴耳机。对讲期间临时静音 FLV 声音，改从 SDK 通道收听，结束后恢复原状态。共用的“开启声音/关闭声音”按钮在对讲期间控制SDK回传声音，不影响麦克风上传。视频画面仍走云平台，可能与实时对讲声音存在延迟差。

“对讲诊断”区分浏览器发送、SDK 已提交（每5秒更新）和回传字节；后台日志记录 camera_id、用户、SDK 错误码、收发字节和停止原因。不记录密码、票据或业务录音。“已提交”不能证明设备扬声器出声，需现场人员确认。无回传字节时检查摄像机拾音和回传能力；浏览器已发送但 SDK 提交为0时查看后台错误；SDK 提交增长而现场无声时检查摄像机扬声器、音量和权限。

取消、关闭弹窗、切换设备、断网、队列积压都停止采音并释放 SDK 会话，不自动重新打开麦克风。SDK 占用或不支持 PCM8k16 时直接报错，不自动回退到此前无法控制大华的天翼云对讲。

## 已完成的验证及边界

- Maven 后端 clean package、前端生产构建通过。
- 63项前端单元测试通过，含分钟级连续重采样和 PCM/G.711 解码。
- 后端离线测试覆盖一次性票据、所有权、设备占用、非法包、SDK提交、回传、取消后迟到登录、队列积压及超时释放。
- Chrome 真正执行 AudioWorklet，使用模拟麦克风与模拟 HTTP/WS 验证开始/结束、回传播放、错误清理、IP绑定保存/解除和权限隐藏。证据：`docs/verification/dahua-talkback-browser.json`。这些不代表已连接实际设备。
- 增量SQL在临时 MySQL 验证重复执行、唯一约束与外键，业务数据库未改动。
- Windows 动态库从项目资源加载并初始化/清理成功，未登录摄像机、未发送音频。
- **Linux 原生库加载、部署网络和真实双向通话尚未验证**，需要部署后以山潘新苑的现场收听确认验收。
