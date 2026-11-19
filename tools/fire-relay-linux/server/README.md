# 公网服务器接收端更新包

配套麒麟1.1切换版，使用原有SSH入口218.94.2.84:18881，将连接转到服务器127.0.0.1:22000。包内包含更新后的ruoyi-admin.jar、独立生产TLS配对文件、受限转发公钥及安装脚本。**这是服务器包，不能放到麒麟电脑**；麒麟包不含服务器TLS私钥。

今晚只在开发机进行验证，没有连接或修改公网服务器。以下步骤在明天可操作服务器时执行。

## 1. 保留现有生产配置，更新后端

先备份服务器正在使用的后端JAR、启动命令和外置配置。将本包ruoyi-admin.jar替换到原来的后端部署位置；原有生产数据库、Redis、文件目录及其他功能配置继续使用现有外置配置。不要把开发电脑的数据库配置覆盖到服务器。

本包仅为转发接收功能增加独立配置，无新增转发数据库表，复用项目原有装置、报文和事件表。服务器现有数据库是否与当前项目版本一致尚未现场核对；若落后于项目，请沿用既有升级流程处理，不要用开发库整体覆盖。

## 2. 安装接收配置

解压本包，在解压目录执行，最后一个参数填写**后端Java进程实际使用的系统账号**：

```sh
sudo sh install-receiver.sh root
```

若后端实际以firecontrol账号运行，应把root替换为firecontrol。脚本将配对文件安装到`/opt/firecontrol-relay-receiver`，只允许本机127.0.0.1:22000接入，不改动正在运行的后端或数据库。

在宝塔或原服务的启动命令中，给JAR之后增加程序参数：

```sh
--spring.config.additional-location=optional:file:/opt/firecontrol-relay-receiver/fire-relay-local.properties
```

若原命令已有同名参数，把这个文件路径用逗号追加到原值。保留原有生产配置参数。随后按原部署方式重启后端，日志应出现：

```text
消防转发TLS接入已启动 127.0.0.1:22000，节点=site-01
```

检查监听：

```sh
ss -lntp | grep ':22000'
```

如果没有监听，先看后端启动错误，不要改公网映射。允许的原始设备网段为10.5.89.0/24和10.5.90.0/24。

## 3. 授权独立SSH转发公钥

默认使用现有root账号，与麒麟production配置一致：

```sh
sudo sh authorize-ssh.sh root
```

它向该用户authorized_keys追加独立公钥，只允许访问`127.0.0.1:22000`，禁止交互命令、终端和其他转发目标；保留已有宝塔登录密钥。无需重启sshd。前提是现有sshd策略允许该账号公钥登录及本地端口转发；若策略禁止，需要管理员按现场策略放行这个用途。

可以改用现有专用账号，脚本参数和麒麟production.ssh.username保持一致。脚本不会创建账号，也不修改sshd_config。

## 4. 在麒麟探测后切换

```sh
sudo sh /opt/firecontrol-relay/run.sh --target production --probe
sudo sh /opt/firecontrol-relay/switch-target.sh production
sudo systemctl restart firecontrol-relay
```

先看到`PROBE_OK target=production`，再执行后两条。检查服务器报文和事件实际出现。开发电脑不需要同时在线。

SSH主机指纹来自本机此前已信任的服务器记录；如果服务器确实更换过主机密钥，应通过管理员可信渠道核对后更新麒麟的production_known_hosts，不能关闭主机校验。

## 回退

麒麟先切回development并重启转发服务。服务器若需回退，恢复之前备份的JAR和启动参数，再按原方式重启。配对目录和新增公钥可保留；要撤销此权限，只删除authorized_keys中注释为firecontrol-relay-production的那一行，保留其他密钥。
