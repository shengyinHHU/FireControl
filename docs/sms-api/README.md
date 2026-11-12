# 调用本项目的短信服务

先阅读 [短信服务接口调用说明](短信服务接口调用说明.md)。管理员只需给调用方 API 根地址、专用账号和密码。

交付文件：

- `FireControl-SMS.postman_collection.json`：导入 Postman，获取 Token → 发送 → 查询。
- `FireControl-SMS.postman_environment.json`：导入后在自己的本地值填写地址、账号、密码和手机号。
- `openapi.json`：OpenAPI 3.0.3 接口定义。
- `sms_client.py`：Python 3 标准库示例客户端，支持保留 requestId 的发送和查询。

新的短信生成新的 requestId；重试同一次发送保留原 requestId。HTTP 200 后仍要检查 `data.taskStatus`，`submitted` 表示平台受理。

真实服务在部署 SQL、启用配置和创建专用账号后可用。管理员步骤见上级目录 `sms-api-admin.md`，无需交给调用方。
