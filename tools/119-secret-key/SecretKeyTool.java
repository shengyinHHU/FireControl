import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;

/** Standalone wrapper around the supplied vendor example. */
public class SecretKeyTool {
    private static final String ALGORITHM = "RSA";
    private static final Integer MAX_ENCRYPT_LENGTH = 117;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX").withZone(ZoneId.of("Asia/Shanghai"));

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--once"))) {
            System.err.println("Usage: java SecretKeyTool.java [--once]");
            System.exit(2);
        }
        boolean once = args.length == 1;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        do {
            Map<String,Object> secretKeyMap = new HashMap<>();
            secretKeyMap.put("account","DIANXIN");
            secretKeyMap.put("password","DIANXIN@123");
            secretKeyMap.put("timeStamp",System.currentTimeMillis() / 1000);
            HashMap<String, Object> reqMap = new HashMap<>();
            reqMap.put("orderSource","DIANXIN");
            // Only JSON serialization uses a local helper instead of Hutool JSONUtil.
            reqMap.put("secretKey",encryptSection(toJsonStr(secretKeyMap)));
            if (reqMap.get("secretKey") == null) {
                throw new IllegalStateException("secretKey 生成失败，请查看上方错误；不要发送 null 密文。");
            }
            if (!once) {
                long seconds = ((Number) secretKeyMap.get("timeStamp")).longValue();
                System.out.println("\n119 接口 secretKey 生成工具（仅在本地计算）");
                System.out.println("固定参数：account=DIANXIN，password=DIANXIN@123，orderSource=DIANXIN");
                System.out.println("生成时间：" + TIME.format(Instant.ofEpochSecond(seconds)));
                System.out.println("timeStamp：" + seconds + "（Unix 秒）");
                System.out.println("按文档的 5 分钟校验窗口，请生成后及时调用；实际结果以服务端为准。");
                System.out.println("\nPOST https://wdsjs.jiangning.gov.cn:9091/zhjn-order/api/api/open/generateToken");
                System.out.println("Content-Type: application/json");
                System.out.println("Body 选择 raw / JSON，复制以下完整内容（含 secretKey）：");
            }
            System.out.println(toJsonStr(reqMap));
            if (once) return;
            System.out.println("\n按回车重新生成；输入 q 后回车退出。工具不会发送请求或保存结果。");
            String command = input.readLine();
            if (command == null || command.trim().equalsIgnoreCase("q")) return;
        } while (true);
    }

    /** Public-key chunk encryption, retaining the supplied example's flow. */
    public static String encryptSection(String plaintext) {
        // JDK equivalent of StringUtils.isEmpty(plaintext).
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            // JDK Base64 replaces the Apache Commons Base64 dependency.
            byte[] decoded = Base64.getDecoder().decode(
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg0Ub6OQqQHo51Ur8wtEu49TJThCjynZdkr3bqFhejJsnr7HHdkOGnLHv+FTHB+/xKpuOFyxqN/gwl3CO7UBxqhflgIgxynTcZZI4RGhKHwxez0NUjr98s5IWgG9pUYcEP07Z8rXJXyKzj8T+fDoqignmwhgtwCDjOkWiKxAS6zpH7gHbHuM/1DVHZfV6dwNl7vphyuylOKiGmiOUqxJ4sGCJtmjLeYx4dKwKqr3pbSKhsb1TqfeUgeVfV1H+iyEDgL7+T0kmd2MKirgCH8hO0St36p+E+xlF1z/1yURd0/KwAeP6cHxzhh+k12ADusB8zzCG4kZk2Lb8hAguIXbC5QIDAQAB");
            RSAPublicKey pubKey =
                (RSAPublicKey)KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] bytes = plaintext.getBytes();
            int inputLen = bytes.length;
            int offLen = 0;
            int i = 0;
            ByteArrayOutputStream bops = new ByteArrayOutputStream();
            while (inputLen - offLen > 0) {
                int length = Math.min(inputLen - offLen, MAX_ENCRYPT_LENGTH);
                byte[] cache = cipher.doFinal(bytes, offLen, length);
                bops.write(cache);
                i++;
                offLen = MAX_ENCRYPT_LENGTH * i;
            }
            bops.close();
            return Base64.getEncoder().encodeToString(bops.toByteArray());
        } catch (Exception e) {
            // Standalone equivalent of the original log.error call.
            System.err.println("加密失败：" + e.getMessage());
            return null;
        }
    }

    // Sufficient for the supplied request maps (String keys, String/Long values).
    // JSON property order is not part of the protocol.
    static String toJsonStr(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append(quote(entry.getKey())).append(':');
            Object value = entry.getValue();
            if (value instanceof Number) json.append(value);
            else if (value instanceof String) json.append(quote((String) value));
            else throw new IllegalArgumentException("Unsupported JSON value");
        }
        return json.append('}').toString();
    }

    private static String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            if (c == '\\' || c == '"') result.append('\\').append(c);
            else if (c < 0x20) result.append(String.format("\\u%04x", (int) c));
            else result.append(c);
        }
        return result.append('"').toString();
    }
}

