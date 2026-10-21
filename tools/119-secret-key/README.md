# 119 接口 secretKey 本地生成工具

双击 run.bat 即可生成，不需要输入账号密码，不需要 Maven 或启动项目。需要 Java 17 或更高版本。

固定使用 account=DIANXIN、password=DIANXIN@123、orderSource=DIANXIN，以及用户提供的 RSA 公钥。已替换为用户最新提供的参数和公钥，接口认证仍以服务端返回为准。

启动时使用当前 Unix 秒时间戳，输出包含 secretKey 的完整 Token 请求 JSON。按回车重新生成，输入 q 后回车退出。工具只在本地计算，不访问网络，不将结果写入文件。

在 Postman 中使用 POST 请求：
https://wdsjs.jiangning.gov.cn:9091/zhjn-order/api/api/open/generateToken

Body 选择 raw → JSON，粘贴工具输出的完整 JSON，Content-Type 为 application/json。此前该路径已实测返回业务 JSON，完整认证仍需要实际调用验证。

文档描述时间戳校验窗口为 5 分钟，建议生成后立即调用，并确保电脑时钟准确。生成的是请求密文，不是 Token。Token 需由接口成功响应返回，文档所述 Token 有效期为 10 分钟。

加密核心尽量保持用户示例原样：encryptSection、ALGORITHM="RSA"、MAX_ENCRYPT_LENGTH=117、Cipher.getInstance(ALGORITHM)、plaintext.getBytes()、while 循环和偏移计算均保留。main 中保留 HashMap、固定参数及 System.currentTimeMillis() / 1000。仅将 JSONUtil、StringUtils、Apache Base64 和日志替换为无第三方依赖的本地/JDK 实现。启动脚本指定 UTF-8，当前固定参数均为 ASCII。RSA 填充有随机性，同一秒生成的密文通常也不同。

只输出一次 JSON：在本目录运行以下 PowerShell 命令。

```powershell
java "-Dfile.encoding=UTF-8" --source 17 .\SecretKeyTool.java --once
```

本工具独立于项目业务代码，不修改数据库或后端配置。



