## 客户接口协议v2.3

## 目录

第一章 发送接口....2
1.1 请求地址....2
1.2 参数说明....2
1.3 返回值....4
第二章 状态报告及上行....4
2.1 请求地址....4
2.2 参数说明....5
2.3 返回值....5

## 第一章 发送接口

## 1.1请求地址

https://ip:port/SMSController/sendSms.do 

## 1.2参数说明

<table><tr><td>参数名称</td><td>含义</td><td>说明</td></tr><tr><td>auth_code</td><td>授权码,由平台提供</td><td></td></tr><tr><td>spid</td><td>企业代码,由平台提供</td><td></td></tr><tr><td>extport</td><td>扩展码,暂不支持</td><td>填“”;</td></tr><tr><td>smsid</td><td>客户短信任务编号</td><td>用于查询,确保唯一;可不填;</td></tr><tr><td>mobiles</td><td>目标手机号列表</td><td>用英文,隔开;</td></tr><tr><td>content</td><td>短信内容(签名+短信内容;例:【T3】您的验证码是874690)</td><td>URLEncoder.encode(content,&quot;utf-8&quot;);如果使用模板方式发送可不填;</td></tr><tr><td>sendtime</td><td>定时发送时间,暂不支持</td><td>填“”;</td></tr><tr><td>timestamp</td><td>时间戳,13位字符串,单位:毫秒</td><td>System.currentTimeMillis()</td></tr><tr><td>templateCode</td><td>模板编码</td><td>平台创建模板后生成模板编码,可不填;若填写则视为使用模板方式发送,否则视为短信内容方式发送;</td></tr><tr><td>templateParam</td><td>{&quot;f0&quot;:&quot;您好&quot;,&quot;f1&quot;:&quot;666999&quot;}</td><td>URLEncoder.encode(templateParam,&quot;utf-8&quot;);模板参数值,json格式字符串,模板中参数从前往后依次用f0~f9代替,例如模板中只有两个参数,则传值中用f0和f1表示参数值即可;模板中没有参数时可不填;使用带参数的模板发送时必填。</td></tr><tr><td>sign</td><td>sign = SHA1(auth_code + spid + export + smsid + mobiles + sendtime + URLEncoder.encode(content,&quot;utf-8&quot;) + timestamp + auth_key + templateCode + URLEncoder.encode(templateParam,&quot;utf-8&quot;))</td><td>sign中content需要URLEncoder.encode(content,&quot;utf-8&quot;);sign中templateParam需要URLEncoder.encode(templateParam,&quot;utf-8&quot;);</td></tr><tr><td>msgfmt</td><td>消息类型</td><td>闪信为24;普通短信为8(短信可不填)</td></tr></table>

```javascript
auth_code=c115f0a1936a4545bec5adc54dd4834d524eb0b1&spid=111111&export=&smsid=&mobiles=130XXXXXXX&content=232312313123131%5B%E7%B3%BB%E7%BB%9F%E7%AE%A1%E7%90%86%E5%91%98%5D&sendtime=&timestamp=1629683352031&sign=9d4245339306ab0585300dad08aa9ce397519c92 
```

```javascript
auth_code=d7c0deee63435e18d5ab1d6c3e1fcab8f242&spid=111111&export=&smsid=&mo 
```

https://qxtapi.js118114.com:9003/SMSController/sendSms.do 

## 参数提交示例：

get提交示例：

## 模板发送方式发送参数示例

https://qxtapi.js118114.com:9003/SMSController/sendSms.do 

22%2C%20%22f1%22%3A%20%22666999%22%7D&sign=71f5529b968a60d727ec6031e9db46 

```json
Post提交示例：
{
  "auth_code":"1195208f9b17d720069c64e690346fe3aa6fdef3",
  "spid":"111111",
  "smsid":"0001",
  "mobiles":"130XXXXXXXX",
  "content":"%E3%80%90%E7%81%AB%E4%BF%A1%E5%95%86%E8%B4%B8%E3%80%91%E7%9F%AD%E4%BF%A1%E9%AA%8C%E8%AF%81%E6%96%B0",
  "sendtime":"",
  "timestamp":"1628576902987",
  "sign":"fe814d1e42b2b18b639555cd4fc109ee9aa32dd4"
}

模板发送方式发送参数示例
{
  "auth_code":"d7c0deeef63435e18d5ab1d6c3e1fcab8f242",
  "spid":"111111",
  "mobiles":"173xxxxxxxxx,153xxxxxxxxx",
  "templateCode":"334283342193822542689573194741",
  "templateParam":"%7B%22f0%22%3A%20%22%E6%82%A8%E5%A5%BD%22%2C%20%22f1%22%3A%20%22666999%22%7D",
  "timestamp":"1749017784589",
  "sign":"71f5529b968a60d727ec6031e9db46"
}
```

上述示例仅供参考。

## C# URL转码需注意：

Java中URL编码所用的 URLEncoder.encode 产生的字符是大写且英文“ ( ”和“) ”是分别转成“%28”和 “%29”，“!”转成“%21”，“+”转成“%2B”,空格转成“+”

而C#中的HttpUtility.UrlEncode产生的字符是小写且部分英文符号并没有转码，所以两者生成的字符不一致，导致系统出错。

//对转码后的字符进行大写转换，不会把参数转换成大写（采用）

```java
public static string GetUpperEncode(string encodeUrl)
{
    var result = new StringBuilder();
    int index = int.MinValue;
    for (int i = 0; i < encodeUrl.Length; i++)
    {
    string character = encodeUrl[i].ToString();
    if (character == "%")
    index = i;
    if (i - index == 1 || i - index == 2)
    character = character.ToUpper();
    result.Append(character);
    }
    return result.ToString();
} 
```

## 1.3 返回值

```json
{"code":1,"text":"success","smsid":"201804271147336901773+45224","phones":"18862976815,18862976816"} 
```

## 说明：

Code 为 1 表示任务提交成功，其他值都是失败；

text 是本次处理描述；

smsid 为平台生成的本次任务编号；，

phones 为正常发送的号码，如号码不在此中，则是无效号码，不会发送，不会有状态报告；

提醒：Code 为 1 仅表示任务提交成功，不表示短信发送成功，如需短信发送回执情况，需客户按照本协议第二章开发回调地址。

## 第二章 状态报告及上行

## 2.1请求地址

由合作客户提供，平台会将数据 post 过去。

如：htt<sub>p</sub>s://i<sub>p</sub>:<sub>p</sub>ort/xxxxxx

## 2.2参数说明

<table><tr><td>参数名称</td><td>含义</td><td>说明</td></tr><tr><td>code</td><td>字符串</td><td>-1 为上行短信,0 为短信发送成功,其他值为短信发送失败</td></tr><tr><td>result</td><td>字符串</td><td>code=-1 时,为上行短信内容,否者为 code 的文本解释</td></tr><tr><td>phone</td><td>字符串</td><td>手机号</td></tr><tr><td>usersmsid</td><td>字符串</td><td>客户自填短信任务编号code=-1 时,为空</td></tr><tr><td>spid</td><td>字符串</td><td>平台给客户分配的企业码</td></tr><tr><td>destId</td><td>字符串</td><td>短信扩展码</td></tr><tr><td>smsid</td><td>字符串</td><td>本平台生成的短信标识,code = -1 为空</td></tr></table>

## 参数提交示例：

```jsonl
code = 0 状态报告
{"usersmsid":"12345678912345","phone":"18812345678","code":"0","result":"DELIVRD","spid":"000000","destId":"1103","smsid":"20190820094355619221+19290"}
code = -1 短信上行
{"usersmsid":"","phone":"18812345678","code":"-1","result":"%E6%94%B6%E5%88%B0","spid":"000000","destId":"1103","smsid":""}
```

## 2.3 返回值

合作客户收到数据后，直接写回一个字符转 SUCCESS