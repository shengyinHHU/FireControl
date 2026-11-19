# 麒麟转发程序 1.1：开发与生产切换

适用于麒麟桌面V10 SP1、aarch64。原生静态程序，无需JDK/Python/Go环境。保留原版部署包，新增这个独立版本，旧的单目标config.json仍能运行。

当前按用户的实际用途采用**一次只向一个目标发送**。默认development：开发电脑；production：公网服务器。没有双路复制、没有离线补传队列。设备失败重连、后端入库后确认的语义保持原样。

| 配置名称 | 路径 |
|---|---|
| development | 麒麟 → 开发电脑10.214.18.130:22000，直接TLS |
| production | 麒麟 → SSH 218.94.2.84:18881 → 服务器127.0.0.1:22000，再做TLS和节点认证 |

两套接收端使用独立证书、节点密钥。生产SSH使用本次单独生成的转发密钥，不复用现有宝塔管理私钥。开发目标继续使用已经调通的原有配对。

## 先保留开发链路

复制并在麒麟解压：

```sh
tar -xzf FireRelay-Kylin-V10-aarch64-switchable.tar.gz
cd FireRelay-Kylin-aarch64-switchable
sh run.sh --check-config
sh run.sh --target development --probe
```

旧程序仍在运行时，不要再启动第二个设备监听实例。`--check-config`、`--probe`不监听2000，不写业务报文；生产探测可以临时建立SSH通道。

手动运行新版本前先停止旧实例，再执行`sh run.sh`。默认向开发电脑发送。

## 选择目标

临时运行某个目标，不修改配置：

```sh
sh run.sh --target development
sh run.sh --target production
```

保存目标供以后启动时使用：

```sh
sh switch-target.sh development
sh switch-target.sh production
```

同一时间只运行一条上述启动命令。切换配置不会接管已经建立的连接；保存后关闭旧实例，再启动程序，设备需自动重连。程序会先验证选中配置的证书指纹和密钥/SSH文件是否齐全，失败时不覆盖原配置。切换前的配置备份为`config.json.before-target-switch`；它只是最近一次备份。

同目录config.json中，`active_target`选择目标，`destinations.development`、`destinations.production`分别保存目标地址、证书指纹、密钥路径和SSH配置。更换服务器接收端时必须使用对应配对，不能只改IP。

## 升级开机服务

在新包目录执行：

```sh
sudo sh install-service.sh
```

更新`/opt/firecontrol-relay`中的程序，保留现场原有监听地址、开发目标、密钥及已存在的目标配置，补充缺失的production配置与独立SSH文件；升级旧版配置时默认保持development。不会修改网卡、路由、防火墙，不启动第二套服务。

服务安装后应操作**安装目录**：

```sh
sudo sh /opt/firecontrol-relay/run.sh --target production --probe
sudo sh /opt/firecontrol-relay/switch-target.sh production
sudo systemctl restart firecontrol-relay
sudo journalctl -u firecontrol-relay -n 50 --no-pager
```

返回开发电脑：

```sh
sudo sh /opt/firecontrol-relay/switch-target.sh development
sudo systemctl restart firecontrol-relay
```

手动模式升级配置也可使用`新程序路径/fire-relay --config 旧目录/config.json --import-targets 新目录/config.json`，但须先把新包的`production-relay.secret`和`ssh`文件复制到旧配置所在目录，且保留旧的`relay.secret`。若不需要保留自定义配置，直接使用新解压目录更简单。

## 明天接入服务器的顺序

1. 将配套`FireRelay-Server-Receiver.tar.gz`复制到服务器，按其中README更新后端、安装接收配置和受限SSH公钥。
2. 服务器确认监听`127.0.0.1:22000`，SSH仍使用原有18881入口。无需新增公网22000映射。
3. 麒麟执行`sh run.sh --target production --probe`，看到`PROBE_OK target=production`后再保存production并重启转发服务。
4. 观察服务器的设备报文、事件及确认；需要调试时可切回development。

production默认SSH用户名为root，与现有管理入口一致。如果明天使用其他现有账号授权，在config.json的production.ssh.username中同步改为该账号。生产模式需要麒麟安装有OpenSSH客户端：`ssh -V`。直接开发模式没有此额外运行依赖。

## 网络要求与限制

麒麟仍为10.5.90.50/29。开发路径沿用已调通的到10.214.18.130的专用路由；生产路径需要麒麟可访问218.94.2.84 TCP18881。若现场默认网关10.5.90.49不能到公网，需要管理员为该公网目标经H3C配置专用路由及相应放行/SNAT，程序不会更改网络。

日志同目录logs/relay.log轮转保存，后台服务也输出到journal。每台装置保留实际观察到的来源IP，两个目标接收端各自认证和管理设备。设备到麒麟之间已经做NAT时无法恢复被隐藏的IP。

断网时不产生假的成功确认，也不会自动把另一目标当作备用来确认设备。没有持久化队列，补报取决于设备重传。此次不连接、更新或重启公网服务器；服务器实机、麒麟ARM64实机和现场公网路由仍需明天验收。本地验证报告为dist/verification-switchable.json及dist/verification-ssh.json。
