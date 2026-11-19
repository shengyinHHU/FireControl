# FireControl 用传设备转发（Windows x64）

转发电脑双击 `FireRelay.exe` 即可运行，无需 JDK、Python 或额外运行库。程序读取 **EXE 所在目录** 的 `config.json`，与双击时的工作目录无关。支持直接 TLS 接入 FireControl，以及经已有 SSH 18881 通道接入。

## 本次接线与地址

```text
用传设备 → 大华交换机 → 转发电脑 10.5.90.50:2000
                         ↓ 同一张网卡，经 H3C 10.5.90.54
                      H3C 上联 10.214.18.131
                         ↓
                  开发电脑 10.214.18.130:22000
```

转发电脑设备网卡：`10.5.90.50`，掩码 `255.255.255.248`（/29），设备网关 `10.5.90.49`。设备目标继续填写 `10.5.90.50:2000/TCP`。其他设备网段仍需经过现场网关正确路由。

单网卡通过已有 H3C 出口上传时，给开发电脑地址增加 `/32` 专用路由，下一跳 `10.5.90.54`；H3C 需允许 `10.5.90.50 → 10.214.18.130 TCP 22000`，并选择正确的源 NAT 或回程路由。SNAT 为 H3C 上联地址时，开发电脑看到的网络来源是 `10.214.18.131`。应用仍按经认证的转发头中的设备原始 IP 识别设备。

开发电脑需手动放行 TCP 22000 的实际转发来源；转发电脑需手动允许设备网段访问 TCP 2000。程序和开机任务脚本 **不更改网卡、路由、防火墙、交换机或休眠设置**。部署人员需确认设备 IP 不冲突、H3C 当前连接和配置与以上记录一致，并关闭转发电脑的自动睡眠。

## 开发电脑接收端

接收端已经集成到 `ruoyi-system`，无需另起一个接收 EXE。与 TCP 2000 直连入口共用 GB26875 分帧、事务入库、装置识别、停用/删除处理、在线状态和确认报文逻辑。

构建脚本生成两套文件，务必区分：

| 文件夹 | 放置位置 | 内容 |
|---|---|---|
| `forwarder` | 复制到转发电脑 | EXE、config.json、节点密钥、开机脚本、说明 |
| `receiver-private` | 保留在开发电脑/接收服务器 | TLS私钥容器、密码、节点密钥、接收配置 |

运行 `python tools/fire-relay/build.py --install-local-receiver` 会生成随机密钥和证书，并将接收配置放到被 Git 忽略的 `RuoYi-Vue-master/config/fire-relay-local.properties`。现有 Spring Boot 配置会自动导入此文件；**重启开发后端**后，日志应显示 `消防转发TLS接入已启动 0.0.0.0:22000`。没有这个本地配置时，转发入口默认关闭，不影响原有部署。

接收配置中的路径是绝对路径；移动 `receiver-private` 后需同步更新。其他部署路径可设置环境变量 `FIRE_RELAY_CONFIG` 指向配置文件。上层外置生产配置若覆盖了相同属性，需要同步核对。

接收配置关键项：

| 配置 | 意义 |
|---|---|
| `fire.relay.enabled` | 是否启用转发接收 |
| `fire.relay.bind-address` / `port` | 开发电脑直接接收时为 `0.0.0.0:22000`；仅SSH接入时建议 `127.0.0.1:22000` |
| `fire.relay.node-id` | 必须与转发端 node_id 一致 |
| `fire.relay.allowed-source-cidrs` | 此节点可报告的设备源网段，默认 `10.5.89.0/24,10.5.90.0/24` |
| `fire.relay.key-store` | PKCS12服务器证书和私钥路径 |
| `fire.relay.key-store-password-file` / `secret-file` | 密码及节点密钥文件路径 |

源IP来自可信现场代理的实际TCP对端，并非隧道本身保留了IP。服务器必须先验证节点，且只接受配置网段内的源IP。转发端若在到达设备之前就被做了源NAT，不能恢复被隐藏的原始IP。同一接收系统仍沿用当前全局来源IP身份模型；不同现场若设备IP重叠，需另行做身份模型升级。

## 转发电脑操作

1. 把 `forwarder` 文件夹复制到固定位置，例如 `D:\FireRelay`，不要复制 `receiver-private`。
2. `config.json` 的 `target.address` 默认 `10.214.18.130`，`target.port` 默认 `22000`。`listen.address` 默认为 `10.5.90.50`，端口 `2000`。
3. 双击 `FireRelay.exe`。窗口持续显示接收端认证状态、设备接入及每次会话的上/下行字节数。按 Ctrl+C 停止；不要关闭窗口后误以为它还在运行。
4. 日志保存到同目录 `logs/relay.log`，最多保留当前文件及5个历史文件，每个最多约5MiB；不会长期记录全部报文原文。完整业务原文继续由 FireControl 入库保存。
5. 修改配置后重新启动程序。配置缺失、未知字段、密钥或证书指纹错误会明确报错，不会降级成明文转发。

可在程序目录打开终端执行（可选）：

```powershell
.\FireRelay.exe --check-config
.\FireRelay.exe --probe
```

`--probe` 只做TLS和节点身份验证，不创建装置或写业务报文。普通 `Test-NetConnection` 只说明TCP握手成功，不能代替此认证探测。

证书采用SHA256固定指纹验证，密钥采用每次连接独立随机挑战的HMAC认证。更换到另一套接收端时，不仅需要修改目标地址，还要使用该接收端配套的密钥和证书指纹。证书/密钥文件不嵌入EXE，不能仅复制EXE而遗漏它们。

## 开机自动运行

确认手动运行成功后关闭手动窗口，以管理员身份打开PowerShell，在程序目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\install-startup.ps1
```

脚本创建并立即启动 Windows 计划任务 `FireControl-Device-Relay`：开机启动、SYSTEM身份、不依赖桌面登录、无运行时长限制、异常退出后重试。普通双击与开机任务只能运行一个实例，端口独占会阻止第二个实例。任务模式没有窗口，查看日志确认状态。

停止/重新启动：

```powershell
Stop-ScheduledTask -TaskName FireControl-Device-Relay
Start-ScheduledTask -TaskName FireControl-Device-Relay
```

移除开机任务：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\install-startup.ps1 -Remove
```

请将程序、config.json和密钥放在受保护的固定目录；以SYSTEM运行的任务目录不能允许普通用户任意修改。SSH模式的密钥必须能由任务身份非交互使用。

## 断线与确认语义

- 每台设备连接对应一条独立TLS业务连接；保留来源IP并隔离字节流，支持TCP拆包、粘包和部分写入。
- 后端完成原有数据库事务后产生确认，原路返回设备；转发电脑不冒充服务器确认，也不主动发送设备控制命令。
- 每方向最多64KiB转发缓存，超限通过TCP背压限制，不无限占内存。无持久化离线队列。
- 链路失败时关闭该设备会话；下一次设备重连时重新建立上游。不会把新上游拼接到旧字节流中。日志中的“上行字节”表示已提交网络栈，不代表服务器入库或设备认可。
- 后台持续认证探测，`reconnect_seconds` 控制探测及SSH重连间隔。它不会主动连到用传设备；设备需要使用TCP客户端模式并支持自动重连。
- 断网补报完整性取决于设备自身的缓存、重连和重传能力，需要实机断网验收；不承诺在设备不重传时零丢失。

## 以后改为服务器：可选SSH模式

服务器部署同版本接收端及配套接收配置，设置为本机 `127.0.0.1:22000`。公网无需增加22000映射。转发电脑 `config.json` 内设置：

- `ssh.enabled=true`；host=`218.94.2.84`、port=`18881`。
- 配置专用SSH账号、private_key_file和known_hosts_file；主机指纹经管理员可信方式核对后放入known_hosts。
- `ssh.remote_address=127.0.0.1`、`remote_port=22000`，`local_port=22100`。
- 使用服务器对应的 `target.certificate_sha256` 和 `relay.secret`；`target.address` 可填公网地址。

SSH模式调用Windows自带OpenSSH `ssh.exe`，不需要JDK，但机器必须有OpenSSH客户端。采用严格主机校验、仅密钥认证和非交互模式，连接失败自动重试。服务器账号需要允许本地端口转发，建议限定到 `127.0.0.1:22000`。既有TCP18881继续提供SSH服务，无需把它改成用传协议端口。直接TLS模式没有OpenSSH依赖。

## 构建与验证（开发人员）

构建环境：Windows x64、Python 3.11+、PyInstaller、cryptography；这些仅在构建电脑需要。输出目录不能已存在，以防覆盖现有配对密钥。

```powershell
python tools/fire-relay/build.py --install-local-receiver
python tools/fire-relay/verify.py
python tools/device-capture/verify_tcp_binding.py
```

后续更新EXE并保留已有证书、密钥和配置：`python tools/fire-relay/build.py --reuse-pair`。

`FireRelay-Windows-x64.zip` 只包含转发端文件，明确排除接收端TLS私钥。验证使用隔离MySQL、随机本地端口、模拟设备，不连接真实用传或触发现场告警。网络链路、重启后运行及真实设备补报需要现场验收。
