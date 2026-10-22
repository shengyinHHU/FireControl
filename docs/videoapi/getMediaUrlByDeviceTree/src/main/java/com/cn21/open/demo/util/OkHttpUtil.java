package com.cn21.open.demo.util;

import okhttp3.*;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author cbx
 * @date 2020/7/3
 **/
public class OkHttpUtil {

    /**
     * 线程池数量：100
     */
    public static final String MAX_IDLE_CONNECTION = "100";

    /**
     * 连接超时：45000ms
     */
    public static final String CINNETC_TIMEOUT = "45000";

    /**
     * 读超时：45000ms
     */
    public static final String READ_TIMEOUT = "45000";

    /**
     * 保存长连接时间：1min
     */
    public static final String KEEP_ALIVE_DURATION = "1";

    private static OkHttpClient httpClient;

    static {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(Long.valueOf(CINNETC_TIMEOUT), TimeUnit.MILLISECONDS)
                .readTimeout(Long.valueOf(READ_TIMEOUT),TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(Integer.valueOf(MAX_IDLE_CONNECTION), Long.valueOf(KEEP_ALIVE_DURATION), TimeUnit.MINUTES)).build();
    }



    public static String sendGet(String url, String param) {
        String result = "";
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);

            Request request = new Request.Builder()
                    .url(realUrl)
                    .header("accept", "*/*")
                    .header("connection", "Keep-Alive")
                    .header("user-agent", "ehome-push")
                    .build();

            Response response = httpClient.newCall(request).execute();
            result = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String sendPost(String url, Map<String, Object> params, Map<String, String> headers) {
        String result = "";
        try {
            // 创建request
            Request.Builder reqBuilder = new Request.Builder();
            // 加入header参数
            addHeaders(reqBuilder, headers);
            // 创建请求body
            RequestBody body = genRequestBody(params);
            URL realUrl = new URL(url);
            Request request = reqBuilder.url(realUrl).post(body).build();
            System.out.printf("请求前,url:%s, params:%s%n", url, params);
            Response response = httpClient.newCall(request).execute();
            result = response.body().string();
            System.out.printf("请求后,result:%s%n", result);
        } catch (Exception e) {
           e.printStackTrace();
        }
        return result;
    }

    private static void addHeaders(Request.Builder reqBuilder, Map<String, String> headers) {
        // 配置header
        reqBuilder.addHeader("accept", "*/*");
        reqBuilder.addHeader("connection", "Keep-Alive");
        if (headers != null) {
            headers.forEach(reqBuilder::addHeader);
        }
    }

    private static RequestBody genRequestBody(Map<String, Object> params) {
        // 组装入参
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null) {
            params.forEach((key, val) -> builder.add(key, String.valueOf(val)));
        }
        return builder.build();
    }


}
