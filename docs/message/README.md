# 短信平台独立联调工具

本目录独立于项目后端、前端和 Maven 配置，仅使用 JDK 11+ 标准库。当前机器已使用 JDK 17.0.12 编译并实测。

本次结论：已跑通本地 HTTPS 调用、任务提交和手机收件。有效组合为 `spid=1000048804`、正文 `【大厂街道办事处】hello`，平台返回 `code=1`、`success`，用户随后确认已收到短信。新提供的 `025C00434993` 用作接口 spid 时返回“系统不存在该用户！”。详见 [TEST-REPORT.md](TEST-REPORT.md)。

## 还缺哪些参数

| 参数 | 当前情况及处理 |
| --- | --- |
| 接口地址 | 使用协议中的 `https://qxtapi.js118114.com:9003/SMSController/sendSms.do`，已验证可以访问。 |
| `auth_code`、`spid`、`auth_key` | 已保存在本地 `sms-local.properties`，成功实测使用原 spid `1000048804` 及原密钥。`auth_key` 只用于本地计算，不随 HTTP 请求发送。 |
| `mobiles` | 已提供。工具支持英文逗号分隔的手机号。 |
| `content` | 当前为 `【大厂街道办事处】hello`。该签名取自用户截图，已实测提交并收件成功。 |
| `smsid` | 首次使用 `0001`；两次补测分别使用 `0002`、`0003`。当前配置已留空，工具每次自动生成新任务编号。 |
| `timestamp` | 工具每次生成 13 位毫秒时间戳。 |
| `sign` | 工具每次按本次全部参数计算 SHA-1，输出 40 位小写十六进制。 |
| `export` / `extport` | 协议表格写 `extport`，公式和 GET 示例写 `export`，POST 示例未列出。扩展码暂不支持，均按空串参与签名；本工具默认提交 `export=""`，可用 `extensionField=extport` 切换字段名。 |
| `sendtime` | 暂不支持定时发送，工具固定传空串。 |
| `templateCode`、`templateParam` | 普通内容发送留空。模板方式需要平台已创建的模板编码；有变量时还需对应参数 JSON。 |
| `msgfmt` | 工具传 `8`，表示普通短信。 |

“短信签名”指正文中的 `【企业名称】` 等已报备文字；请求参数 `sign` 是 SHA-1 摘要，两者不是同一个字段。截图中的“短信签名 ID”也不等于 `sign` 或 `templateCode`，本接口按协议直接在正文中带上短信签名。

截图显示 `【大厂街道办事处】` 的状态为“已提交，待初审”，但实际使用该正文签名的请求已受理且手机收到短信。后台当前审核状态未单独查询，不能仅凭这张截图推断实时接口结果。

## sign 的计算

严格按以下顺序拼接，无分隔符：

```text
SHA1(
  auth_code + spid + export + smsid + mobiles + sendtime
  + URLEncoder.encode(content, "UTF-8")
  + timestamp + auth_key + templateCode
  + URLEncoder.encode(templateParam, "UTF-8")
)
```

- 使用 Java `URLEncoder`：空格为 `+`，百分号编码使用大写十六进制。
- 配置文件填写原始正文、原始模板 JSON，由工具编码一次。参与签名和实际提交的编码字符串完全相同。
- HTTP 使用协议 POST 示例中的 JSON，正文已经编码，不再对整个 JSON 做表单编码。
- `auth_key` 不在请求 JSON 中。`msgfmt` 不参与签名。
- `timestamp`、`smsid`、正文或其他参与字段变化后都必须重算，旧 sign 不能直接复用。

## 使用方法

本机账户参数已写入 `sms-local.properties`。其他环境可复制 `sms-example.properties` 并填写参数，配置文件按 UTF-8 读取。

```powershell
Set-Location 'D:\javaprogram\FireControl\docs\message'

# 默认仅生成请求和 sign，不发送网络请求。
.\run.ps1

# 固定时间戳和任务编号，复现成功请求的 sign；仍不发送。
.\run.ps1 --timestamp 1794376300303 --smsid 0003
```

本机配置已填写实测成功的正文 `content=【大厂街道办事处】hello`。需要修改正文时可使用 `--content` 覆盖原始正文，并保留平台可用的短信签名。若走模板方式，配置 `templateCode` 和所需的 `templateParam`，并按平台要求设置正文；本次未测试模板方式。

真实提交一次的命令如下；当前配置会自动生成新任务编号：

```powershell
.\run.ps1 --send
```

不使用脚本也可以直接运行 Java 源文件：

```powershell
java '-Dfile.encoding=UTF-8' SmsProbe.java --help
java '-Dfile.encoding=UTF-8' SmsProbe.java
```

工具每次发送仅有一个 POST 请求，不自动重试，不跟随重定向。连接超时为 10 秒，请求超时为 30 秒，使用 JDK 默认的 HTTPS 证书和主机名校验。超时或网络中断时不能推断服务端是否收到，需先核对平台记录。

## 本地文件

- `SmsProbe.java`：请求构建、编码、sign 计算、HTTPS 调用和记录。
- `run.ps1`：切换到本目录后启动 Java 源文件。
- `sms-example.properties`：可共享的无密钥配置模板。
- `sms-local.properties`：当前账户本地配置，已被本目录 `.gitignore` 排除。
- `local-results/<运行时间-随机编号>/request.json`：完整请求，无 `auth_key`。
- 同目录的 `summary.json`：timestamp、sign、HTTP 状态、平台 code、耗时或异常。
- 发送时另存 `response.txt`、`response-headers.txt`；包含平台原始返回。

本地配置及原始请求、响应包含账户或号码信息，均被本目录 `.gitignore` 排除；测试报告隐藏手机号。没有修改根目录 `.gitignore`。

## 如何判断成功

协议中 `code=1` 仅表示短信任务提交成功。最终短信送达要看接收手机，或配置协议第二章的状态回调：回调 `code=0` 表示发送成功，`code=-1` 是短信上行。回调服务需要合作方提供可被平台访问的地址，本次独立提交测试未搭建回调服务。
