# 原项目的服务器更新材料

**服务器功能已集成在原项目 `ruoyi-admin.jar` 中，按之前宝塔Java项目的方式替换JAR并重启即可，不另建或启动接收程序。** 包内脚本只用于首次配置、授权，不是常驻程序。

推荐部署方式：把本包 `project-config/config` 合并到原Java项目工作目录的 `config`，项目会自动加载接收和对讲配置，原启动命令保留。证书和密钥与麒麟已配对，勿覆盖现场不同的同名文件。详细步骤见仓库 `RuoYi-Vue-master/config/README-fire-relay.md`；本包也包含同一份 `PROJECT-DEPLOYMENT.md`。

下面保留使用 `/opt/firecontrol-relay-receiver` 外置配置的安装方式，供已经按该路径安装的环境使用；两种配置放置方式选择一种即可。

此包与 `FireRelay-Kylin-V10-aarch64-intercom.tar.gz` 配套，保留原生产TLS/HMAC配对，新增独立对讲SSH公钥和后端地址映射。不要将本包的receiver-private目录发送给麒麟电脑。

## 安装前

1. 服务器需为Linux **x86-64**：`uname -m` 应为 `x86_64`。麒麟可以是ARM64。
2. 保留现有JAR、启动命令、外部配置、数据库和环境变量备份。此版无新增SQL表；原有对讲功能需要已有 `SQL/fire_video_dahua_talk.sql` 表。
3. `18881` 继续为SSH入口。服务器本机 `22000` 用于用传接收，`33777` 用于对讲，不增加公网映射。
4. 确认摄像机仍为 `10.5.90.150:37777`。修改时同步麒麟config、下面的properties和公钥permitlisten。

## 配置文件和SDK

解压后进入本目录，以后端实际运行用户替换下面的 `root`：

```sh
sha256sum -c SHA256SUMS
sudo sh install-receiver.sh root
sudo sh install-intercom.sh root
sudo sh authorize-ssh.sh root
sudo sh authorize-intercom.sh root
```

`authorize-intercom.sh` 只增加包内专用公钥，不修改sshd配置。它检查转发策略，并拒绝会强制公网监听的 `GatewayPorts=yes`。如服务器存在按来源IP匹配的SSH策略，第二个参数填写服务器实际观察到的麒麟出口IPv4，再复查授权；仅检查默认来源不能覆盖所有Match策略。

对讲公钥禁止shell、PTY、代理转发，反向监听限定 `127.0.0.1:33777`；本地转发目标限定接收端 `127.0.0.1:22000`。它与用传SSH公钥独立。sshd全局/Match策略也须允许remote forwarding；有问题时脚本停止并说明，不自动修改策略。

`install-intercom.sh` 安装 `/opt/firecontrol-relay-receiver/fire-intercom.properties`，内容为：

```properties
fire.video.dahua.tunnels.shanpan.device-host=10.5.90.150
fire.video.dahua.tunnels.shanpan.device-port=37777
fire.video.dahua.tunnels.shanpan.local-port=33777
```

后台“山潘新苑”的对讲绑定继续使用 **10.5.90.150、37777、原登录配置**。服务器内部在调用SDK时改用127.0.0.1:33777。前端仍只允许绑定专网摄像机IP，不把127.0.0.1开放给普通绑定表单。该映射只加载在服务器；不要复制到仍需直连摄像机的开发环境。

Linux SDK位于包内 `native/linux64`。可将此目录完整保留在固定安装位置，例如 `/opt/firecontrol-dahua/native/linux64`，随后在原后端服务环境中增加：

```sh
export LD_LIBRARY_PATH=/opt/firecontrol-dahua/native/linux64${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
```

此路径需实际复制到位，并确保后端用户能读取。先运行 `LD_LIBRARY_PATH=该目录 ldd 该目录/libdhnetsdk.so` 检查缺失依赖，不能把配套SSL库覆盖到系统目录。SDK随JAR也已打包；外部目录供Linux动态链接器查找配套依赖。systemd运行时需在原服务中设置相同环境，终端export不会影响已运行的服务。

保留已有摄像机凭据配置，或通过服务环境设置 `FIRE_DAHUA_USER`、`FIRE_DAHUA_PASSWORD`。此包不包含摄像机密码。

## 更新JAR并启动

将包内 `ruoyi-admin.jar` 替换到原后端JAR位置，使用原启动方式和所有原有参数，在JAR文件名之后增加或合并：

```text
--spring.config.additional-location=optional:file:/opt/firecontrol-relay-receiver/fire-relay-local.properties,optional:file:/opt/firecontrol-relay-receiver/fire-intercom.properties
```

如果原来已有 `spring.config.additional-location`，把这两个文件追加到原值，不覆盖原配置。脚本不自动替换正在运行的JAR，不重启生产服务，不访问业务数据库。

网页端对讲需要HTTPS/WSS或localhost安全上下文。原入口 `http://218.94.2.84:18880` 本身不满足浏览器麦克风要求。请沿用已配置的HTTPS访问地址，并将网页实际Origin加入 `FIRE_DAHUA_ORIGINS`，反向代理须支持 `/fire/video/dahua/socket` WebSocket升级。若没有HTTPS，可在开发电脑用已授权的SSH会话将网站转到localhost调试；这项访问与麒麟摄像机隧道是两条不同的链路。

## 现场验收

1. 后端启动无配置错误，本机22000正常监听。
2. 麒麟执行 `--target production --probe-cameras` 与 `--target production --probe`，分别确认摄像机端口及用传接收认证，再切换production并重启麒麟服务。
3. 服务器 `ss -lntp | grep -E ':(22000|33777)\b'` 应显示本机地址，33777不应监听0.0.0.0。监听成功只证明反向端口建立，不证明摄像机登录或音频成功。
4. 网页选“山潘新苑”，检查原对讲绑定及摄像机凭据，允许麦克风，点击开始对讲。后端应出现 `Dahua tunnel ... local=127.0.0.1:33777`，随后SDK登录和对讲会话成功。
5. 分别确认现场能听见网页讲话、网页能听见现场回音；结束后设备会话释放，用传消息仍继续入库。

此版已在本地通过OpenSSH双向数据/重连测试和后端离线测试，尚未以真实摄像机在跨公网路径验证声音。如果SDK报告额外UDP或端口需求，应先保留日志抓包后适配。旧直连成功记录证明设备/P模式可用，不证明新网络路径已经验收。

回退时：麒麟切回development；服务器恢复原JAR及原启动配置，移除新追加的对讲配置引用，保留原数据库和业务配置。
