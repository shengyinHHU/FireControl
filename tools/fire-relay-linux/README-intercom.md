# 麒麟 ARM64：用传上报与摄像机对讲 1.2

此前实测成功的摄像机为 `10.5.90.150:37777`，“山潘新苑”，P模式。此版本复用原SDK及浏览器语音实现；麒麟仅承担网络通道，不需要在ARM上运行大华x86-64 SDK。视频画面沿用现有平台播放链路，此通道用于大华SDK对讲。

```text
用传设备 → 麒麟10.5.90.50:2000 → SSH -L → 服务器127.0.0.1:22000
浏览器麦克风 ↔ HTTPS/WSS后端 ↔ 大华SDK ↔ 服务器127.0.0.1:33777
                                               ↕ 独立SSH -R
                                  麒麟 ↔ 摄像机10.5.90.150:37777
```

两个SSH进程均由麒麟主动连接 `218.94.2.84:18881`，分别使用独立密钥，断线自动重连。对讲通道申请失败不会关闭用传通道。SSH中断时正在进行的对讲可能结束，网络恢复后需重新点击开始对讲。不是离线音频缓存，不重播旧语音。

## 升级麒麟

先完成配套 `FireRelay-Server-Intercom` 包中的服务器安装，再切换生产。开发路径可以继续使用原程序。旧Windows、第一版麒麟和1.1切换版安装包均保留。

解压本包并进入目录：

```sh
sha256sum -c SHA256SUMS
sudo sh install-service.sh
```

安装程序保留已有的监听地址、目标、配对密钥与当前选择，只在缺少时补充 `production.intercom`，并安装独立对讲SSH私钥。若已有同名对讲配置，保留现场值。升级会重启本地转发服务，已有设备连接需重连。

检查摄像机端口，再检查服务器接收端：

```sh
sudo sh /opt/firecontrol-relay/run.sh --target production --probe-cameras
sudo sh /opt/firecontrol-relay/run.sh --target production --probe
```

`CAMERA_TCP_OK` 仅代表麒麟到摄像机SDK端口可连接；`PROBE_OK` 仅代表服务器用传接收端TLS/HMAC验证成功。两者都不表示摄像机SDK登录或声音已验证。

服务器配置就绪后切换：

```sh
sudo sh /opt/firecontrol-relay/switch-target.sh production
sudo systemctl restart firecontrol-relay
sudo journalctl -u firecontrol-relay -n 80 --no-pager
```

回到原开发路径：

```sh
sudo sh /opt/firecontrol-relay/switch-target.sh development
sudo systemctl restart firecontrol-relay
```

只在生产目标默认启用对讲通道。开发目标保持已调通的用传接收路径；如需让开发电脑也经反向SSH对讲，需为开发电脑另配SSH入口与对应映射。

## 配置和网络

`config.json` 的 `destinations.production.intercom`：

```json
{
  "enabled": true,
  "private_key_file": "ssh/intercom_ed25519",
  "cameras": [
    {"name": "shanpan", "device_address": "10.5.90.150", "device_port": 37777, "server_port": 33777}
  ]
}
```

最多8台，每台独立服务器本机端口。修改或新增时需同时调整服务器映射及SSH公钥的 `permitlisten`，并重启麒麟服务。`enabled:false` 可单独关闭对讲转发。

麒麟仍为 `10.5.90.50/29`。`10.5.90.150` 位于另一个子网，须通过现场网关正确路由，不能因“同在交换机”就假定可达。用 `--probe-cameras` 检查。生产入口TCP18881也须可达；程序不修改IP、路由、防火墙或交换机。

SDK模式沿用之前成功的P模式。此实现透明转发指定TCP端口；若现场SDK仍尝试UDP或其他端口，需要据SDK错误与抓包继续补充，不能仅以监听成功判断对讲成功。

## 已验证与待验证

本地验证覆盖真实OpenSSH客户端反向转发、并发双向二进制数据、SSH掉线重连、对讲端口被拒时用传路径继续工作，以及Java对讲地址映射和原有会话生命周期。ARM64为静态编译产物；实际麒麟、真实摄像机跨公网的SDK登录与双向声音需现场验收。今晚未连接或更新公网服务器。
