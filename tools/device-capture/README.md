# 大华 DH-HY-FAM-1000 TCP 接收与确认工具

默认监听 10.5.90.50:2000，按 GB/T 26875.3—2011 的包结构分帧并返回传输确认，完整记录双向数据。仍为联调工具，尚未解析具体火警业务、写数据库或更新前端。协议确认表示原始数据已保存，不表示报警已处理。

## 运行

设备选择 TCP 客户端，设备 IP 10.5.90.49、掩码 255.255.255.252，目标地址 10.5.90.50、端口 2000。电脑有线网卡配置 10.5.90.50 和相同掩码，双方接同一交换机。

PowerShell 执行：

```powershell
cd D:\javaprogram\FireControl\tools\device-capture
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-capture.ps1
```

先停止旧采集程序，避免占用端口。需要 Python 3.9 或以上，无第三方依赖；脚本优先使用明确指定的 Python，其次 Python Launcher、当前用户 Codex 附带的 Python、PATH 中的 Python。

默认最多运行 5 分钟或接收 100 MiB，按 Ctrl+C 提前结束。ExecutionPolicy Bypass 仅针对该次 PowerShell 进程，不改变系统策略。

```powershell
# 延长到 10 分钟
.\run-capture.ps1 -DurationSeconds 600

# 指定 Python
.\run-capture.ps1 -PythonExe "C:\实际安装目录\python.exe"

# 如需恢复纯采集模式（不发送确认）
.\run-capture.ps1 -ReceiveOnly
```

启动后应显示 GB26875 frame confirmation enabled。看到 Connected、Frame 和 ACK sent 后说明收到完整包并已向本机 TCP 栈提交确认，仍需观察设备是否认可。触发一次装置模拟火警，记录操作时间，保留运行一段时间后停止，将整个本次输出目录交给开发人员。

## 分帧与确认规则

- TCP 是字节流，支持单帧分多次接收、一次接收多个帧；按长度定位包尾，应用数据内部出现 @@ 或 ## 不会提前截断。
- 校验启动符 @@、应用数据长度 0..1024、总长度 30+应用数据长度、算术校验和及结束符 ##。
- 仅对主版本 1、命令 02（发送数据）的完整校验通过包返回 03（确认）；用户版本保持原值，流水号保持原值，源/目的协议地址交换，使用本机当前时间，应用数据为空，重新计算校验和。
- 支持此次实测的零应用数据 02 报文并返回确认，但不直接将其认定为火警或厂家定义的心跳。
- 重复包仍逐次确认，并在日志中标记 duplicate。该标记只比较近期完整包字节，不代表业务去重。
- 收到 03、控制/查询命令、其他命令或不支持的主版本只记录，不主动应答，避免确认循环；不主动发送控制、复位、消音或校时。
- 损坏包不发送成功确认，记录原因并搜索下一个启动符。当前诊断工具不发送否认报文，也不声称实现完整协议服务端。
- 不完整帧等待后续数据，连接结束时记录 incomplete_frame。候选帧残留有长度上限。
- 原始接收数据和解析记录写入并同步到磁盘后，才排队确认。排队与实际发送分别记录；非阻塞发送支持部分写入和暂时不可写。
- 达到总时长、字节限制、按 Ctrl+C、设备断开或发送队列达到上限时，尚未发出的确认会记录 unsent_confirmations，不伪报已发送。
- 本工具不校验具体业务对象结构，也不解释火警状态。正式生产接入应在业务持久化完成后按正式策略确认。
- 接收时间带本机时区，报文中的设备时间单独记录；不会修改设备时间。确认使用本机时间不等于发送校时命令。

字段规则依据标准第 6.3、6.6 节：
https://baike.jbufa.com/index.php/城市消防远程监控系统第3部分报警传输网络通信协议_GBT_26875.3-2011
国家标准登记：
https://openstd.samr.gov.cn/bzgk/std/newGbInfo?hcno=B96F1614665EF81C42164A3218073251

## 输出

每次新建 captures/时间戳/，不覆盖旧采集，默认不提交 Git。

| 文件 | 内容 |
| --- | --- |
| events.jsonl | 双向原始数据、完整帧字段、校验失败、确认排队/完成、连接、错误和结束记录 |
| connection_0001.bin 等 | 每连接收到的完整原始字节流（兼容第一版文件名） |
| connection_0001.tx.bin 等 | 每连接实际发送的字节流；未发送的排队内容不写入 |
| summary.json | 接收/发送字节数、完整帧数、校验失败候选数、完整确认发送数、退出原因 |

received/sent 记录均有时间、方向、连接、各方向独立 offset、length 和 hex。frame_received 包含流水号、版本、设备时间及其原字节、协议源/目的地址、命令、应用数据长度/内容和 duplicate。协议地址不是 IP 地址。

disconnected 包含双向 SHA-256。confirmation_queued 仅表示排队；sent 才表示实际 socket.send 成功提交的字节，confirmation_sent 表示整条确认均已提交。它们不能证明对端已经接收或认可。

原始报文可能含设备身份和位置，不要提交到公开仓库。把本次整个目录和模拟火警触发时间一起提供。

## 排查

```powershell
Get-NetIPAddress -AddressFamily IPv4
Get-NetTCPConnection -LocalPort 2000 -ErrorAction SilentlyContinue
```

- 绑定失败：确认本机拥有 .50 地址，端口未被旧程序占用。
- 无 Connected：确认设备 TCP 客户端设置、交换机链路和防火墙。
- 有 Frame，但无 ACK sent：看是否纯采集模式、命令/主版本不支持、发送错误或运行已结束。
- 有 ACK sent 仍重传：请提供完整双向记录，需核对设备是否认可返回格式；离线测试不能代替大华实机验证。
- 只有空应用数据：说明设备通信已建立，尚不能生成真实火警；核对装置模拟火警操作。

必要时用管理员 PowerShell 添加仅允许该设备的临时防火墙规则；脚本不会自行修改防火墙：

```powershell
New-NetFirewallRule -DisplayName "FireControl TCP Capture 2000" -Direction Inbound -Action Allow -Protocol TCP -LocalAddress 10.5.90.50 -LocalPort 2000 -RemoteAddress 10.5.90.49 -Profile Any

# 调试结束删除
Remove-NetFirewallRule -DisplayName "FireControl TCP Capture 2000"
```

设备网关填自身 IP 不合理；同网段通信无需经过网关，本工具不修改网络配置。

## 开发验证

从项目根目录执行：

```powershell
python -m unittest discover -s tools/device-capture/tests -v
```

测试仅使用 127.0.0.1 临时端口和此次采样报文，不连接设备、不占用 2000。覆盖拆包、粘包、1024 字节载荷、校验失败及重同步、确认地址/序号/时间/校验、重复包、非应答命令、部分写入、双向日志、纯采集、超时、容量限制和端口冲突。
