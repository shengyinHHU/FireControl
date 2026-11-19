# 跟随原项目部署：麒麟转发接收和摄像机对讲

服务器上的用传接收、大华SDK对讲和网页接口都在本项目的同一个 `ruoyi-admin.jar` 中。`FireRelayServer` 是Spring管理的接收组件，跟随后端启动/停止；大华SDK由原对讲服务调用。服务器无需再启动一个接收程序或对讲程序。

此前“服务器更新包”中的JAR就是本项目构建结果，其余是证书、配置和首次SSH授权材料。麒麟电脑才单独运行ARM64转发程序。

## 原部署方式继续使用

在 `RuoYi-Vue-master` 构建：

```sh
mvn -pl ruoyi-admin -am -DskipTests package
```

将 `ruoyi-admin/target/ruoyi-admin.jar` 上传到宝塔中原Java项目的JAR位置，沿用原项目、运行账号、数据库、外部prod配置和启动命令，然后在原项目中重启。原有 `--spring.profiles.active=druid,prod` 参数保留。不创建第二个Java项目，也不新增服务器端业务服务。

## 首次补充生产配对配置

为避免将生产私钥写入代码仓库，证书和配对密钥保留在服务器配置文件中。项目已支持自动读取工作目录下的 `config/fire-relay-local.properties`，无需为了此功能新增Java启动参数。

配套材料位于 `tools/fire-relay-linux/dist/FireRelay-Server-Intercom/project-config/config`。将其中的 `config` 目录合并到**宝塔原Java项目的工作目录**，保留服务器原有其他配置文件：

```text
原Java项目工作目录/
├── ruoyi-admin.jar
└── config/
    ├── fire-relay-local.properties
    └── fire-relay-private/
        ├── receiver.p12
        ├── keystore-password.secret
        └── relay.secret
```

`fire-relay-local.properties` 同时包含接收端和对讲映射配置。生产证书、密钥与已提供的麒麟包配对，不要重新生成。若服务器已经有不同的同名配置，请先合并，不覆盖现场配对或业务配置。不要用生产文件覆盖当前开发电脑的 `config/fire-relay-local.properties`。

目录和文件需允许原后端运行用户读取，例如现有账号 `www`。证书和密钥不放到Nginx网站根目录，不提交Git。若原Java项目已有其他外置 `config` 路径，保持原路径并合并相同配置；也可通过服务环境 `FIRE_RELAY_CONFIG` 指向此properties文件。

**相对路径以Java项目的工作目录为基准，不以properties文件所在目录为基准。** 例如当前服务器的JAR在 `/opt/firecontrol/app/ruoyi-admin.jar`，配对材料放在兄弟目录 `/opt/firecontrol/config`，则密钥路径应写成：

```properties
fire.relay.key-store=/opt/firecontrol/config/fire-relay-private/receiver.p12
fire.relay.key-store-password-file=/opt/firecontrol/config/fire-relay-private/keystore-password.secret
fire.relay.secret-file=/opt/firecontrol/config/fire-relay-private/relay.secret
```

项目也兼容从工作目录的 `../config/fire-relay-local.properties` 导入配置。新增或修改配置后，必须在原宝塔Java项目中重启才会生效。重启前确认后端运行账号可读取三个配对文件；启动成功应出现127.0.0.1:22000监听，不能只以8080正常判断接收端已启用。

## SSH和SDK的首次准备

麒麟经现有 `218.94.2.84:18881` SSH入口建立通道。第一次须为对应SSH账号添加配套公钥：

```sh
sudo sh authorize-ssh.sh root
sudo sh authorize-intercom.sh root
```

上面的脚本在配套材料目录执行；`root` 替换为麒麟配置中的实际SSH用户名。这是SSH授权配置，只需首次处理，不常驻运行。服务器端口22000和33777供本机访问；对讲通道由麒麟主动建立。

大华Java接口和原生SDK已经通过Maven打进项目。服务器需Linux x86-64。Linux配套动态库目录在 `RuoYi-Vue-master/vendor/dahua-netsdk/native/linux64`，配套材料也包含同样的 `native/linux64`；原后端服务环境的 `LD_LIBRARY_PATH` 应包含其实际安装目录，用 `ldd libdhnetsdk.so` 检查系统依赖。不要另起SDK服务。

继续使用原摄像机凭据配置，或在原后端环境中设置 `FIRE_DAHUA_USER`、`FIRE_DAHUA_PASSWORD`。网页对讲需要HTTPS或localhost，以及正确的 `FIRE_DAHUA_ORIGINS` 和WebSocket代理配置。

## 项目运行后的检查

1. 原后端启动成功，日志显示消防转发TLS接入监听127.0.0.1:22000。
2. 麒麟验证摄像机端口和服务器认证，切换production；服务器出现127.0.0.1:33777的SSH反向监听。
3. 网页“山潘新苑”对讲绑定继续使用10.5.90.150:37777，项目内部转成127.0.0.1:33777。点击原话筒按钮验证现场和网页两边的声音。
4. 用传信息仍由同一后端写入原业务数据库。

本地已经验证编译、Java接收及入库、对讲映射、双向SSH数据和断线重连。真实摄像机经服务器的SDK登录及声音仍需现场验收。
