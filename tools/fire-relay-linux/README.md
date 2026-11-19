# 用传设备转发：麒麟桌面 V10 SP1，aarch64

这是与 Windows 版独立保存的 **Linux ARM64 原生版**。目标机器不需要 JDK、Python、Go、pip 或联网安装依赖。程序使用同目录的 `config.json`，协议与 FireControl 已集成的接收端完全相同，继续保留每台装置的原始来源IP及双向确认。

## 快速开始

把 `FireRelay-Kylin-V10-aarch64.tar.gz` 复制到麒麟电脑，在终端执行：

```sh
tar -xzf FireRelay-Kylin-V10-aarch64.tar.gz
cd FireRelay-Kylin-aarch64
sh run.sh --check-config
sh create-desktop.sh
```

之后可双击桌面的“用传设备转发”。第一次可能需要选择“允许启动/信任”；打开的终端窗口会持续显示运行状态。关闭窗口会停止手动运行的程序。也可直接执行 `sh run.sh`；按 Ctrl+C 停止。

包内配置已经与开发电脑的现有接收端配对，**不要替换成 config.example.json**。默认：

| 项目 | 配置 |
|---|---|
| 设备接收地址 | `10.5.90.50:2000/TCP` |
| 转发目标 | 开发电脑 `10.214.18.130:22000/TCP` |
| 节点 | `site-01` |
| 日志 | 同目录 `logs/relay.log`，每份约5MiB，最多当前及5份历史日志 |

开发电脑需要重启修改后的 FireControl 后端并加载现有 `fire-relay-local.properties`。无需重新生成证书、修改数据库或另外启动接收EXE。Windows部署包及其证书/密钥不会被Linux构建覆盖，两版共享同一配对身份，**现场只选择其中一版运行**。

直接TLS模式的认证探测：`sh run.sh --probe`。它不会登记装置或写业务报文。TCP端口能连通不代表证书和节点认证成功；应看到 `PROBE_OK`。

## 网络仍按原方案

麒麟电脑的设备网卡：IP `10.5.90.50`，前缀 `/29`，掩码 `255.255.255.248`，设备网关 `10.5.90.49`。设备作为TCP客户端连接 `10.5.90.50:2000`。

沿用已有“大华交换机 → H3C”出口时，`10.214.18.130/32` 的专用路由下一跳为 `10.5.90.54`。H3C 需放行TCP22000并配置SNAT或正确回程；开发电脑防火墙需允许实际转发来源。设备其他网段仍按现场网络经 `10.5.90.49` 访问。

程序及安装脚本不修改网卡、路由、防火墙、VLAN或休眠设置。请在麒麟“网络设置”中核对IP及路由，并在“电源管理”中关闭自动睡眠。端口2000不是特权端口，手动运行不需要root；网络和防火墙配置需要管理员处理。

## 开机后台常驻

先确认手动运行正常，然后按 Ctrl+C 停止手动实例，再执行：

```sh
sudo sh install-service.sh
```

程序安装到 `/opt/firecontrol-relay`，由专用低权限账号 `fire-relay` 运行。systemd服务开机启动，不依赖桌面登录，程序退出后5秒重试，日志可通过下列命令查看：

```sh
systemctl status firecontrol-relay --no-pager
sudo journalctl -u firecontrol-relay -n 50 --no-pager
sudo systemctl restart firecontrol-relay
sudo systemctl stop firecontrol-relay
```

后台服务运行后，修改的是 `/opt/firecontrol-relay/config.json`，修改完重启服务；不要只修改下载目录里另一份配置。再次执行安装脚本会更新程序、保留已有配置和密钥。

安装后的桌面状态入口，以普通桌面用户执行：

```sh
sh /opt/firecontrol-relay/create-desktop.sh
```

双击时，若后台服务已经运行，只显示状态，不会重复启动。详细服务日志可能需要管理员权限。

移除开机服务：`sudo sh /opt/firecontrol-relay/install-service.sh --remove`，程序、配置、密钥及日志保留。

## 配置与连接语义

- 修改同目录 `config.json` 的 `target.address`、`target.port`，重启生效。切换另一套接收端，还需要该接收端配套的证书指纹及节点密钥，不能仅更换IP。
- 证书SHA256指纹验证为必选；节点用随机挑战HMAC认证。错误密钥、错误证书不会降级成明文。
- 原始设备来源IP经过认证后传给后端，复用现有解析、装置管理及事务入库逻辑。设备入站之前被NAT隐藏的原始IP无法恢复。
- 确认来自后端，转发程序不冒充服务器确认。数据库失败、链路中断时不能伪报成功。
- 每台设备独立连接，有限缓冲和TCP背压；失败后结束该会话，等待设备重连，不把新上游拼接到旧连接。后台会持续探测目标。
- 没有持久化离线队列，断网补报取决于设备自身缓存和重传能力，需现场验收。

以后使用服务器SSH18881时，将 `ssh.enabled` 设为true，配置已有SSH账号、私钥及known_hosts。此可选模式需要系统的 `ssh` 命令；直接TLS模式没有此依赖。后台账号需可读相关SSH文件，建议放在 `/opt/firecontrol-relay/ssh`，由root与fire-relay组管理，不放在被服务隔离的个人home目录。服务器需允许转发至本机 `127.0.0.1:22000`，并运行同版本接收端。

## 构建与验证

开发环境使用Go编译，`CGO_ENABLED=0`、`GOOS=linux`、`GOARCH=arm64`、`GOARM64=v8.0`；产物不依赖glibc或动态解释器。构建脚本复用之前Windows包中的config和节点密钥，不生成新身份，也不会打包接收端私钥。

当前开发主机不是麒麟ARM64。通用协议逻辑、原生Linux执行及与实际Java接收端的联调结果见构建目录的验证报告；最终麒麟ARM64实机的网卡、桌面启动、systemd和设备补报仍需现场验证。
