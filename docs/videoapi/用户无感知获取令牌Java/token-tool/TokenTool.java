import com.cn21.open.demo.util.ByteFormat;
import com.cn21.open.demo.util.HmacSHATool;
import com.cn21.open.demo.util.XXTea;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class TokenTool {
    public static void main(String[] args) {
        try {
            Properties config = new Properties();
            try (var reader = Files.newBufferedReader(Path.of("config.properties"), StandardCharsets.UTF_8)) {
                config.load(reader);
            }
            String appId = config.getProperty("appId", "").trim();
            String secret = config.getProperty("appSecret", "").trim();
            if (appId.isEmpty() || secret.isEmpty()) throw new IllegalArgumentException("Missing appId/appSecret in config.properties");
            String timestamp = Long.toString(System.currentTimeMillis());
            String plain = "grantType=vcp_189";
            String params = XXTea.encrypt(plain, "UTF-8", ByteFormat.toHex(secret.getBytes(StandardCharsets.UTF_8)));
            if (!plain.equals(XXTea.decrypt(params, "UTF-8", ByteFormat.toHex(secret.getBytes(StandardCharsets.UTF_8))))) {
                throw new IllegalStateException("XXTea round-trip check failed");
            }
            String signature = HmacSHATool.encodeHmacSHA256(appId + "3" + params + timestamp + "v1.0", secret);
            if (signature.length() != 64) throw new IllegalStateException("Invalid HMAC signature");
            if (Arrays.asList(args).contains("--self-test")) {
                System.out.println("PASS: configuration, vendor XXTea round-trip, HMAC signature. No network request sent.");
                return;
            }
            Map<String, String> form = new LinkedHashMap<>();
            form.put("appId", appId);
            form.put("version", "v1.0");
            form.put("clientType", "3");
            form.put("timestamp", timestamp);
            form.put("signature", signature);
            form.put("params", params);
            StringJoiner body = new StringJoiner("&");
            form.forEach((key, value) -> body.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));
            System.out.println("AppID: " + appId);
            System.out.println("POST https://vcp.21cn.com/open/oauth/getAccessToken");
            System.out.println("apiVersion=2.0, version=v1.0, clientType=3, grantType=vcp_189");
            var request = HttpRequest.newBuilder(URI.create("https://vcp.21cn.com/open/oauth/getAccessToken"))
                .timeout(Duration.ofSeconds(45))
                .header("apiVersion", "2.0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Files.writeString(Path.of("result.json"), response.body(), StandardCharsets.UTF_8);
            System.out.println("HTTP " + response.statusCode());
            System.out.println(response.body());
            System.out.println("Response saved to result.json. Check business code: code=0 indicates success.");
            System.out.println("If denied by IP whitelist, register this network's public outbound IP with the provider.");
            if (response.statusCode() < 200 || response.statusCode() >= 300) System.exit(2);
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.err.println("Check network, provider IP whitelist and config.properties. No automatic retry.");
            System.exit(1);
        }
    }
}
