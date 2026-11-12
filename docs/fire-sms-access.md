# 项目短信功能

## 使用

拥有 `fire:sms:send` 及对应联系人来源查看权限的用户，可以在场所联系人、地图场所详情、查岗岗位列表/配置、大屏查岗卡片和当前查岗弹窗中，点击“拨号”旁的“短信”。

弹窗从后台重新读取当前联系人，只显示这个联系人登记的号码。一个有效手机号自动选中，多个号码必须明确勾选。座机、空号码和已停用岗位不可发送。填写正文后，可直接核对完整正文和接收号码，再点“发送短信”。签名 `【大厂街道办事处】` 由后台固定添加。

成功提交后保留本次结果。平台任务受理不等于送达；目前没有接入自动回执，因此显示“未收到回执”。平台明确拒绝会展示原因并保留正文；响应丢失后只能先查询本次提交结果，不直接重发。

“短信通知 → 发送记录”使用真实数据库，在所有构建模式下都不会回退到演示记录。查看权限为 `fire:sms:list`，沿用部门/本人数据范围。原“通知规则”仍为原有独立功能，本次没有启用规则自动发送。

## 首次安装及启动

1. 在业务数据库执行 `SQL/fire_sms.sql`。脚本只新增表和权限，可重复执行，不包含测试号码、密钥或演示记录。
2. 在角色管理中按需要分配 `fire:sms:send`、`fire:sms:list`；发送还要求 `fire:places:list` 或 `fire:inspections:list`，并校验对应数据范围。
3. 服务器配置使用 `RuoYi-Vue-master/config/fire-sms-local.properties`。本机已将先前实测成功的账户参数写入此文件，并通过 Git 忽略。其他服务器复制同目录 `fire-sms-example.properties` 后填写密钥。该文件位于资源目录之外，不会打包进 JAR。
4. 默认从 `RuoYi-Vue-master` 工作目录启动后端。IDE、服务或其他工作目录可设置环境变量 `FIRE_SMS_CONFIG` 为这个本地配置文件的绝对路径。正常的 Spring 环境变量 `FIRE_SMS_AUTH_CODE`、`FIRE_SMS_AUTH_KEY` 等也可覆盖配置。
5. 重新编译、启动后端，并更新前端构建。没有配置时服务默认关闭，页面会明确提示，不模拟发送成功。

未自动修改现有业务数据库，未重启当前运行的业务进程。本次自动化验证使用隔离 MySQL、环回 HTTP 和浏览器受控接口，不向真实联系人发短信。

## 配置

```properties
fire.sms.enabled=true
fire.sms.spid=1000048804
fire.sms.auth-code=由平台提供
fire.sms.auth-key=由平台提供
fire.sms.signature=\u3010\u5927\u5382\u8857\u9053\u529e\u4e8b\u5904\u3011
fire.sms.endpoint=https://qxtapi.js118114.com:9003/SMSController/sendSms.do
fire.sms.max-content-length=1000
```

`max-content-length` 是应用端限制，包含签名，不代表平台计费分段或已核实的平台长度上限。当前采用普通内容短信，模板方式未启用。实际使用的接口 spid 是 `1000048804`，不是网页账号 `025C00434993`。

## 接口与记录

- `GET /fire/sms/recipient?type=placeContact&id=联系人编号&parentId=场所编号`
- `GET /fire/sms/recipient?type=inspectionPost&id=岗位编号`
- `POST /fire/sms/send`：type、id、parentId、expectedPhone、signature、phones、body、requestId。
- `GET /fire/sms/requests/{requestId}`：发送者查询自己的同一请求，不要求记录查看权限。
- `GET /fire/sms/list`：分页、日期、提交状态、联系人/手机号/场所关键字查询。

后台重新解析来源、执行既有 DataScope，再核对预览时的登记电话和签名，以及所选号码是否属于该联系人。不接受任意手机号、演示人员或演示网格来源。

任务在请求外部平台前持久化，`sender_user_id + request_id` 唯一。相同编号、相同内容返回原结果；相同编号更改内容拒绝。提交期间的并发重试只能看到已有记录，不会再次调用平台。即使平台受理后记录更新失败，原请求仍存在；60 秒后未完成的提交按“结果待确认”展示。HTTP 调用不自动重试、不跟随重定向，默认校验 HTTPS 证书。

真实状态：`pending` 正在提交、`submitted` 已提交平台、`partial` 部分号码已提交、`failed` 提交失败、`unknown` 提交结果待确认。送达状态独立保存为 `unconfirmed`；未来接入可信平台回执后再更新。平台拒绝前的本地校验失败不制造发送记录，弹窗展示“未提交”及原因。

请求签名、时间戳及平台任务编号由后端生成。authKey 不出现在前端、请求日志或发送记录中。短信 API 跳过通用前端防重提交的 sessionStorage 缓存，由后台请求编号保证幂等，以免在浏览器持久保存号码和正文。

## 验证

```powershell
python tools/sms/verify_project.py
Set-Location RuoYi-Vue3
npm run test:fire
# PLAYWRIGHT_PATH 指向已安装的 playwright 包时可直接使用：
node tests/fire/sms.browser.cjs
npm run build:prod
```

隔离集成验证覆盖真实 MySQL/MyBatis/DataScope、迁移重复执行、环回请求编码与签名、来源及号码范围、防并发重复提交、部分受理、失败和不确定结果、提交后数据库异常、记录数据范围。浏览器验证覆盖实际 Vue 交互、号码和正文核对、校验、预览、重复点击、失败保持、查询恢复、脏草稿关闭、慢响应、查岗入口、角色权限及手机宽度。记录在 `docs/verification/sms-*.json`，界面预览为同目录 `sms-*.png`。
