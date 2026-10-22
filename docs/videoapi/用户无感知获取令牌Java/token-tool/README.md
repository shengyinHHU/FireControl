# 天翼视联 Token 调试工具

双击 `run.cmd`，等待显示 HTTP 状态码和接口原始响应。需要 JDK 11 或以上，使用 javac/java，无需 Maven 或下载依赖。

已把应用信息填写到本地 `config.properties`，需要更换时编辑该文件。该文件以及返回结果已加入本目录 `.gitignore`。

工具直接编译并复用运营商 ByteFormat、HmacSHATool、XXTea 源码，按照 BaseDemo.apiGen 生成请求：
- POST https://vcp.21cn.com/open/oauth/getAccessToken
- Header apiVersion: 2.0
- form 表单 appId、version=v1.0、clientType=3、timestamp（毫秒）、signature、params
- params：XXTea 加密 grantType=vcp_189，密钥按运营商源码使用 AppSecret 的 UTF-8 字节
- signature：HMAC-SHA256(appId + clientType + params + timestamp + version, AppSecret)，大写十六进制

只获取一次 Token，不运行原示例的空 refreshToken 刷新及定时任务。响应保存至 result.json，业务是否成功请检查 code；HTTP 200 不代表业务成功。响应含 Token 时请妥善保存。保留运营商原始响应，不擅自假定返回数据需要 RSA 解密。

PowerShell 运行：
```powershell
.\run.ps1
.\run.ps1 -SelfTest
```
自检验证配置、运营商 XXTea 加解密以及签名生成，不请求网络。

网络重置/超时：检查本机网络或代理；返回白名单错误：将当前网络公网出口 IP 配置到供应商应用白名单；返回应用/权限错误：核对应用订阅及凭据。工具保持 HTTPS 证书校验，不自动重试。
