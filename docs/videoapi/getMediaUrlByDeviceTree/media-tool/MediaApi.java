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

public class MediaApi {
    public static void main(String[] args) {
        try {
            Properties config = new Properties();
            try (var reader = Files.newBufferedReader(Path.of("../../用户无感知获取令牌Java/token-tool/config.properties"), StandardCharsets.UTF_8)) {
                config.load(reader);
            }
            String appId = config.getProperty("appId", "").trim();
            String secret = config.getProperty("appSecret", "").trim();
            if (appId.isEmpty() || secret.isEmpty()) throw new IllegalArgumentException("Missing appId/appSecret in config.properties");
            String timestamp = Long.toString(System.currentTimeMillis());
            String plain = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (args.length == 1 && args[0].equals("--decrypt")) {
                System.out.println(XXTea.decrypt(plain, "UTF-8", ByteFormat.toHex(secret.getBytes(StandardCharsets.UTF_8))));
                return;
            }
            if (args.length != 1 || !args[0].startsWith("/open/token/")) throw new IllegalArgumentException("Expected token API path");
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
            var request = HttpRequest.newBuilder(URI.create("https://vcp.21cn.com" + args[0]))
                .timeout(Duration.ofSeconds(45))
                .header("apiVersion", "2.0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
            System.out.println(response.body());
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.err.println("Check network, provider IP whitelist and config.properties. No automatic retry.");
            System.exit(1);
        }
    }
}
