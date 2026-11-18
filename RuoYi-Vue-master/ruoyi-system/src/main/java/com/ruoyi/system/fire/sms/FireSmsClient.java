package com.ruoyi.system.fire.sms;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import tools.jackson.databind.ObjectMapper;

/** v2.3 JSON POST; deliberately has no retry and never logs credentials or signing input. */
@Component
public class FireSmsClient {
    private final FireSmsProperties config;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
    public FireSmsClient(FireSmsProperties config, ObjectMapper json) { this.config = config; this.json = json; }
    public record Result(String status, String code, String message, String taskId, String acceptedPhones) { }

    public void ready() {
        if (!config.isEnabled() || blank(config.getAuthCode()) || blank(config.getAuthKey()) || blank(config.getSpid()))
            throw new ServiceException("短信服务尚未配置，请联系管理员");
        URI uri = URI.create(config.getEndpoint());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null)
            throw new ServiceException("短信服务地址配置无效");
        if (blank(config.getSignature()) || !config.getSignature().matches("【[^】]+】")
                || config.getSignature().length() > 64 || config.getMaxContentLength() < 20 || config.getMaxContentLength() > 4000)
            throw new ServiceException("短信签名或长度配置无效");
    }

    protected URI endpoint() { return URI.create(config.getEndpoint()); }

    public Result send(String mobiles, String content, String smsid) {
        try {
            String timestamp = Long.toString(System.currentTimeMillis());
            String encoded = URLEncoder.encode(content, StandardCharsets.UTF_8);
            String sign = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(
                    (config.getAuthCode() + config.getSpid() + smsid + mobiles + encoded + timestamp + config.getAuthKey())
                            .getBytes(StandardCharsets.UTF_8)));
            var body = new LinkedHashMap<String, String>();
            body.put("auth_code", config.getAuthCode()); body.put("spid", config.getSpid());
            body.put("export", ""); body.put("smsid", smsid); body.put("mobiles", mobiles);
            body.put("content", encoded); body.put("sendtime", ""); body.put("timestamp", timestamp);
            body.put("templateCode", ""); body.put("templateParam", ""); body.put("sign", sign); body.put("msgfmt", "8");
            var request = HttpRequest.newBuilder(endpoint()).timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json; charset=UTF-8").header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 || response.body().length() > 65536)
                return new Result("unknown", "", "平台响应异常，提交结果待确认，请核对平台记录", "", "");
            Map<?, ?> parsed = json.readValue(response.body(), Map.class);
            String code = text(parsed.get("code")), message = safe(text(parsed.get("text")));
            String taskId = text(parsed.get("smsid")), phones = text(parsed.get("phones"));
            if (!code.matches("-?\\d+")) return new Result("unknown", "", "平台响应格式异常，提交结果待确认", "", "");
            if ("1".equals(code)) {
                var accepted = new LinkedHashSet<>(Arrays.asList(phones.split(",")));
                if (!accepted.containsAll(Arrays.asList(mobiles.split(","))))
                    return new Result("partial", code, "平台仅受理部分号码，请核对已受理号码", taskId, phones);
                return new Result("submitted", code, "短信已提交平台，送达情况以手机或回执为准", taskId, phones);
            }
            return new Result("failed", code, message.isBlank() ? "平台拒绝提交" : message, taskId, "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result("unknown", "", "请求中断，提交结果待确认，请核对平台记录", "", "");
        } catch (Exception e) {
            return new Result("unknown", "", "未取得可靠的平台响应，提交结果待确认，请核对平台记录", "", "");
        }
    }
    private String safe(String text) {
        for (String secret : List.of(config.getAuthCode(), config.getAuthKey()))
            if (!blank(secret)) text = text.replace(secret, "[已隐藏]");
        return text.substring(0, Math.min(500, text.length()));
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String text(Object value) { return value == null ? "" : value.toString(); }
}
