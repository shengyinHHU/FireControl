import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Isolated HTTPS SMS probe. JDK 11+, no Maven or project dependencies. */
public final class SmsProbe {
    private static final Pattern RESPONSE_CODE =
            Pattern.compile("\"code\"\\s*:\\s*\"?(-?\\d+)\"?(?=\\s*[,}])");
    private static final DateTimeFormatter DIRECTORY_TIME = DateTimeFormatter
            .ofPattern("uuuuMMdd-HHmmss-SSS").withZone(ZoneId.of("Asia/Shanghai"));

    public static void main(String[] args) {
        try {
            System.exit(run(args));
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(2);
        }
    }

    private static int run(String[] args) throws Exception {
        Map<String, String> options = new LinkedHashMap<>();
        boolean send = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--help".equals(arg)) {
                System.out.println("java SmsProbe.java [--config sms-local.properties] [--send]");
                System.out.println("  [--smsid ID] [--content RAW_TEXT] [--timestamp MILLIS]");
                System.out.println("Default: dry run. --timestamp is permitted only for dry runs.");
                System.out.println("--send submits exactly one request, with no automatic retry.");
                return 0;
            } else if ("--send".equals(arg)) {
                send = true;
            } else if ("--config".equals(arg) || "--smsid".equals(arg)
                    || "--content".equals(arg) || "--timestamp".equals(arg)) {
                if (++i >= args.length) {
                    throw new IllegalArgumentException("Missing value for " + arg);
                }
                options.put(arg, args[i]);
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        if (send && options.containsKey("--timestamp")) {
            throw new IllegalArgumentException("--send requires a fresh timestamp; omit --timestamp");
        }

        Path configPath = Path.of(options.getOrDefault("--config", "sms-local.properties"))
                .toAbsolutePath().normalize();
        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            config.load(reader);
        }

        URI endpoint = URI.create(required(config, "endpoint"));
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null
                || endpoint.getRawFragment() != null) {
            throw new IllegalArgumentException("endpoint must be an HTTPS URL without credentials/query/fragment");
        }
        String authCode = required(config, "auth_code");
        String spid = required(config, "spid");
        String authKey = required(config, "auth_key");
        String mobiles = required(config, "mobiles");
        if (!mobiles.matches("1\\d{10}(,1\\d{10})*")) {
            throw new IllegalArgumentException("mobiles must contain 11-digit mobile numbers separated by commas");
        }
        String extensionField = config.getProperty("extensionField", "export").trim();
        if (!"export".equals(extensionField) && !"extport".equals(extensionField)) {
            throw new IllegalArgumentException("extensionField must be export or extport");
        }
        String smsid = options.getOrDefault("--smsid", config.getProperty("smsid", "")).trim();
        if (smsid.isEmpty()) {
            smsid = "probe-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String rawContent = options.getOrDefault("--content", config.getProperty("content", ""));
        String templateCode = config.getProperty("templateCode", "").trim();
        String rawTemplateParam = config.getProperty("templateParam", "");
        if (rawContent.isEmpty() && templateCode.isEmpty()) {
            throw new IllegalArgumentException("Either content or templateCode is required");
        }
        if (templateCode.isEmpty() && !rawTemplateParam.isEmpty()) {
            throw new IllegalArgumentException("templateParam requires templateCode");
        }
        String timestamp = options.getOrDefault("--timestamp", Long.toString(System.currentTimeMillis()));
        if (!timestamp.matches("\\d{13}")) {
            throw new IllegalArgumentException("timestamp must contain 13 digits (milliseconds)");
        }
        String extension = "";
        String sendtime = "";
        String encodedContent = encode(rawContent);
        String encodedTemplateParam = encode(rawTemplateParam);

        // Exact v2.3 order. No separators, no HMAC, and no second URL encoding.
        // auth_key stays local and is NOT included in the HTTP request.
        String sign = sha1(authCode + spid + extension + smsid + mobiles + sendtime
                + encodedContent + timestamp + authKey + templateCode + encodedTemplateParam);

        Map<String, String> request = new LinkedHashMap<>();
        request.put("auth_code", authCode);
        request.put("spid", spid);
        request.put(extensionField, extension);
        request.put("smsid", smsid);
        request.put("mobiles", mobiles);
        request.put("content", encodedContent);
        request.put("sendtime", sendtime);
        request.put("timestamp", timestamp);
        request.put("templateCode", templateCode);
        request.put("templateParam", encodedTemplateParam);
        request.put("sign", sign);
        request.put("msgfmt", "8");
        String requestJson = json(request);

        Path outputDir = configPath.getParent().resolve("local-results")
                .resolve(DIRECTORY_TIME.format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8));
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("request.json"), requestJson + "\n", StandardCharsets.UTF_8);
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("mode", send ? "send" : "dry-run");
        summary.put("endpoint", endpoint.toString());
        summary.put("smsid", smsid);
        summary.put("mobiles", mobiles.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        summary.put("timestamp", timestamp);
        summary.put("sign", sign);
        summary.put("encodedContent", encodedContent);
        summary.put("encodedTemplateParam", encodedTemplateParam);

        System.out.println("Mode      : " + summary.get("mode"));
        System.out.println("Endpoint  : " + endpoint);
        System.out.println("SMS ID    : " + smsid);
        System.out.println("Mobiles   : " + summary.get("mobiles"));
        System.out.println("Timestamp : " + timestamp);
        System.out.println("Sign      : " + sign);
        System.out.println("Artifacts : " + outputDir);
        if (templateCode.isEmpty() && !rawContent.matches("(?s).*【[^】]+】.*")) {
            System.out.println("Notice    : content has no documented bracketed SMS signature.");
        }

        if (!send) {
            summary.put("outcome", "prepared-not-sent");
            writeSummary(outputDir, summary);
            System.out.println("No HTTP request sent. Add --send to submit once.");
            return 0;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(java.time.Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();
        Instant started = Instant.now();
        summary.put("attemptStartedAt", started.toString());
        summary.put("outcome", "attempt-started");
        writeSummary(outputDir, summary);
        try {
            HttpResponse<String> response = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            summary.put("elapsedMs", Long.toString(java.time.Duration.between(started, Instant.now()).toMillis()));
            summary.put("httpStatus", Integer.toString(response.statusCode()));
            Files.writeString(outputDir.resolve("response.txt"), response.body(), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("response-headers.txt"), response.headers().toString(), StandardCharsets.UTF_8);
            Matcher code = RESPONSE_CODE.matcher(response.body());
            String platformCode = code.find() ? code.group(1) : "";
            summary.put("platformCode", platformCode);
            boolean accepted = response.statusCode() >= 200 && response.statusCode() < 300
                    && "1".equals(platformCode);
            summary.put("outcome", accepted ? "task-accepted-delivery-unverified" : "platform-or-http-rejected");
            writeSummary(outputDir, summary);
            System.out.println("HTTP      : " + response.statusCode());
            System.out.println("Response  : " + response.body());
            System.out.println(accepted
                    ? "Task accepted. Delivery must be checked via receipt callback or the recipient."
                    : "Request was not accepted. Inspect response.txt before changing parameters.");
            return accepted ? 0 : 3;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            summary.put("elapsedMs", Long.toString(java.time.Duration.between(started, Instant.now()).toMillis()));
            summary.put("outcome", "transport-error-delivery-unknown");
            summary.put("exceptionType", e.getClass().getName());
            summary.put("exceptionMessage", String.valueOf(e.getMessage()));
            writeSummary(outputDir, summary);
            System.err.println("Transport failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.err.println("No automatic retry. Check platform history before resending after an uncertain response.");
            return 4;
        }
    }

    private static String required(Properties config, String key) {
        String value = config.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String sha1(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(40);
        for (byte item : digest) {
            hex.append(String.format(java.util.Locale.ROOT, "%02x", item & 0xff));
        }
        return hex.toString();
    }

    private static void writeSummary(Path directory, Map<String, String> summary) throws IOException {
        Files.writeString(directory.resolve("summary.json"), json(summary) + "\n", StandardCharsets.UTF_8);
    }

    private static String json(Map<String, String> values) {
        StringBuilder result = new StringBuilder("{\n");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (index++ > 0) {
                result.append(",\n");
            }
            result.append("  ").append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
        }
        return result.append("\n}").toString();
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"': result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t': result.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        result.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) ch));
                    } else {
                        result.append(ch);
                    }
            }
        }
        return result.append('"').toString();
    }
}
