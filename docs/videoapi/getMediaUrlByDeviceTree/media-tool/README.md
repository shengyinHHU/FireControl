# 设备树与视频流调试

复用运营商 Java 的 XXTea、HMAC-SHA256 实现和接口参数。JDK 11+，无需 Maven。
自动读取此前 token-tool/config.properties 和 result.json，不重新获取 Token。

双击 run.cmd 查询最后一级区域，在首个包含设备的区域查询第一页，选第一个设备请求 RTSP/HLS/RTMP，并解密地址。
企业主账号由用户提供；原示例为占位手机号，不可直接使用。可运行：

```powershell
.\run.ps1 -EnterpriseUser '企业主账号手机号'
.\run.ps1 -EnterpriseUser '企业主账号手机号' -DeviceCode '真实设备编码'
```

results 下保存接口响应和解密后的流地址，已通过 .gitignore 排除。业务 code 不为 0 时输出原始错误并停止相关步骤。
获取地址成功不代表播放成功，地址有时效，应及时用 VLC 等播放器验证。
