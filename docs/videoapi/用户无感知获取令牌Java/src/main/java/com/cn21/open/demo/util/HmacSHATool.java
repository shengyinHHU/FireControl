package com.cn21.open.demo.util;

/**
 * @author cbx
 * @date 2020/7/3
 **/

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
public class HmacSHATool {

    public static String encodeHmacSHA256(String plantText, String appSecret){
        try {
            byte[] data = ByteFormat.hexToBytes(ByteFormat.toHex((plantText).getBytes()));
            byte[] key = ByteFormat.hexToBytes(ByteFormat.toHex(appSecret.getBytes()));

            SecretKey secretKey = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance(secretKey.getAlgorithm());
            mac.init(secretKey);
            byte digest[] = mac.doFinal(data);
            return ByteFormat.toHex(digest);
        }catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String[] args) {
        String testStr = "helloworld";
        String result = encodeHmacSHA256(testStr, "test");
        System.out.println(result);
    }

}
