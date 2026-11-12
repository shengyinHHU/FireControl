# 短信开放服务：管理员部署与账号配置

调用方只需要 API 根地址、专用账号和密码；不需要登记项目。调用说明和 Postman 文件在 `docs/sms-api`，对外交付 ZIP 不包含本文件、平台密钥或真实账号凭据。

## 1. 发布前准备

本次代码提供开放服务入口，配置默认关闭。本机已为 localhost 的 dachang 数据库执行增量脚本，创建 `sms_api` 专用账号（部门 100，只绑定短信接口调用者角色），并生成随机 20 位密码。账号资料在 Git 忽略文件 `work/sms-open-private/sms_api-local-credentials.json`；可直接导入的本机 Postman 环境在同目录 `FireControl-SMS.local.postman_environment.json`。这两个文件包含密码，只供管理员和实际调用者使用，不在公开 ZIP 中。

后端已打包为 `RuoYi-Vue-master/ruoyi-admin/target/ruoyi-admin.jar`。本机验证使用临时回环监听端口，验证结束退出；日常使用请从 `RuoYi-Vue-master` 目录执行 `java -jar ruoyi-admin/target/ruoyi-admin.jar`，默认 API 根地址为 `http://localhost:8080`。现有业务改动也会包含在整个项目打包产物中，正式发布前应按项目原有流程核对。

另一台部署服务器仍需按下面步骤执行 SQL、配置并创建自己的账号；本机创建的账号不会自动出现在其他数据库。

备份数据库后，针对项目实际使用的数据库依次执行：

1. `SQL/fire_sms.sql`（已执行过也可重复执行）。
2. `SQL/fire_sms_open.sql`。

脚本开头有 `USE dachang`，若实际数据库名称不同请先改为实际名称。开放脚本创建额度表、请求元数据表，以及权限 `fire:sms:open:send`、`fire:sms:open:query` 和角色 `sms_api_client`（显示名“短信接口调用者”）。脚本不会创建默认密码，也不会覆盖既有账号。角色若已存在且被加过额外权限，需在若依移除额外权限；专用接口会拒绝权限过多的账号。

使用 `RuoYi-Vue-master/config/fire-sms-example.properties` 配置服务器。已有本机 `config/fire-sms-local.properties` 为 Git 忽略文件，保留此前验证有效的平台参数，并已补充开放服务配置和随机 Token 密钥；不要分发此文件。确认生产启动目录及 `spring.config.import` 能加载对应文件，或通过环境变量设置。

必需配置示意：

```properties
fire.sms.enabled=true
fire.sms.spid=1000048804
fire.sms.auth-code=由管理员填写
fire.sms.auth-key=由管理员填写
fire.sms.signature=\u3010\u5927\u5382\u8857\u9053\u529e\u4e8b\u5904\u3011
fire.sms.open.enabled=true
fire.sms.open.max-recipients=10
fire.sms.open.minute-limit=30
fire.sms.open.daily-limit=500
fire.sms.open.login-limit=30
```

上面的 Unicode 转义在 Java properties 中解码为 `【大厂街道办事处】`。请保留转义写法，直接写入 UTF-8 中文可能被 properties 加载器按其他编码解析，导致签名配置无效。

`token.secret` 必须为至少 64 个随机字节的 Base64 字符串；原项目弱默认值会让开放入口返回 503。可通过环境变量 `TOKEN_SECRET` 提供（application.yml 已支持），也可写入仅服务端读取的配置文件。已有本地配置中若有 token.secret，会优先于 application.yml 的环境变量占位值；生产统一使用一种来源，避免误覆盖。

生成命令（仅在管理员终端运行，不要将输出交给调用方）：

```powershell
python -c "import secrets,base64; print(base64.b64encode(secrets.token_bytes(64)).decode())"
```

该密钥是本项目统一 JWT 签名密钥，并非短信平台 authKey。**更换它并重启会使原有后台登录 Token 失效，需要重新登录**；多实例必须使用同一个持久配置值，不要每次启动随机生成。

可选来源限制 `fire.sms.open.allowed-ips[0]=192.0.2.10/32`，支持 IP / CIDR，默认不限制。依据实际 TCP 对端 IP 检查，忽略 X-Forwarded-For。通过反向代理时对端可能是代理；如需限制最终调用方，应在可信网关配置来源限制。登录限频也按直接对端计数，因此共用代理的调用方会共享此限额。

重新打包并部署后端，保证 MySQL 和 Redis 可用、后端能访问短信平台 HTTPS 9003 端口。对外使用 HTTPS，将 `/fire/sms/open/**` 的转发超时配置为至少 45 秒；网关不要对发送请求自动换编号重试。不要在访问日志中记录 Authorization 或登录请求体。

## 2. 在若依创建账号

管理员登录后台：

1. 系统管理 → 角色管理，确认“短信接口调用者”存在、启用，权限仅为开放发送和开放查询两个。
2. 系统管理 → 用户管理 → 新增用户，设置用户名、昵称、所属部门、强密码，状态正常。
3. 只绑定“短信接口调用者”角色，不附加普通业务或管理员角色；不使用超级管理员 userId=1。
4. 将 API 根地址、该账号和密码交给调用方。不同系统可分别建账号，用于独立额度、日志和停用；也可使用同一账号，但会共享额度及本账号查询记录。

用户名 2～20 字符，密码 5～20 字符，建议随机 20 位强密码。可以使用 `tools/sms/create_api_user.py` 辅助开通：通过若依管理员现有 Token 调用管理接口，自动寻找指定角色、拒绝覆盖已有用户名、生成随机密码，并将凭据写入 Git 忽略目录 `work/sms-open-private`。

```powershell
$env:RUOYI_BASE_URL='https://实际API根地址'
$env:RUOYI_ADMIN_TOKEN='管理员后台现有Token（不含Bearer前缀）'
python tools/sms/create_api_user.py --username sms_api --dept-id 100
```

这个命令会创建真实用户。只在部署好 SQL 后运行；部门 ID 请填写真实部门。若管理员 Token 无效或无用户 / 角色管理权限，脚本会停止；可改用界面操作。生成的凭据文件含密码，属于管理员私密资料，不放入对外交付 ZIP。遇到超时或返回不可靠，脚本保留 prepared 凭据文件；先检查若依用户列表，不要盲目重复创建。

## 3. 验收和日常管理

用 Postman 对新服务账号执行获取 Token。第一次发送使用自有测试手机，并核对手机是否收到；提交会真实消耗平台额度。无需提供平台 authCode / authKey 给调用方。

开放账号每次调用都重新查数据库用户、角色和菜单权限。停用用户、停用 / 删除角色、撤权、附加其他有效角色或重置密码，都会使旧 Token 在后续调用被拒绝。恢复后重新获取服务 Token。普通后台登录 Token 无法直接调用开放发送接口。

默认额度以收件号码数计数：每账号上海自然分钟 30、自然日 500；失败 / unknown 也占用，重复请求不重复占用。额度和记录在同一事务中持久化，多个实例共享。没有上线额度管理 UI，修改限额使用配置并重启。不要删除历史 `fire_sms_record` / `fire_sms_open_meta` 记录破坏 requestId 去重保证；应根据实际审计和保存要求另行规划归档。

管理员具有相应短信记录权限时，可从原有短信记录页查看开放提交（来源为“短信接口 / 用户名”）；专用账号无全局记录列表权限。当前尚未接入送达回执，提交状态和送达状态独立，submitted 不保证到达手机。

## 4. 验证与交付

`python tools/sms/verify_project.py` 回归原联系人短信功能；`python tools/sms/verify_open.py` 使用独立 MySQL、独立 Redis、真实 HTTP Spring Security / JWT / BCrypt 和本机模拟短信平台，不读取平台真实凭据、不操作业务数据库、不发送真实短信。

`python tools/sms/smoke_local.py` 临时启动打包后的完整应用，验证本机专用账号登录、本人记录查询、参数拒绝、全局列表越权拒绝和退出登录。它读取本机数据库 / Redis 并产生正常登录审计记录，但不会提交有效的短信发送请求。设备监听、119 同步和视频服务在该次临时启动中关闭。

`python tools/sms/provision_local.py --dept-id 100` 仅用于本机首次准备，使用项目配置的 localhost 数据库和 Java BCrypt；拒绝覆盖已有用户名 / 私密凭据文件。执行前将相关表结构和权限行保存在 `work/sms-open-private/local-before-provision.json`。正式服务器优先使用前述若依界面或管理员 API 开通工具。

`python tools/sms/build_api_bundle.py` 生成公开的 Postman、OpenAPI 文件和 `docs/sms-api-caller.zip`。源码修改后重新运行以同步交付包。账号凭据、管理员说明、真实请求文件和本地环境文件不会放入 ZIP。
