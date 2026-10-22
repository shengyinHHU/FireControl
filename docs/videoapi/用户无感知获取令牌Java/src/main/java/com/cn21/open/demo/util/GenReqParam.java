package com.cn21.open.demo.util;

import com.cn21.open.demo.common.Constant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 生成参数
 * @author lyf15
 * @date 2023/7/12
 **/
public class GenReqParam {

    /**
     * 对应文档：门户文档中心-接口规范
     * 请注意，这里的的公共参数请改成自己的应用对应的参数{@link Constant}
     * 生成参数，参数加密，添加签名等
     * @param specialParam 接口具体入参
     * @return
     */
    public static Map<String,Object> apiGen(Map<String, Object> specialParam) throws Exception{
        // 公共参数
        String appId = Constant.APP_ID;
        String version = Constant.VERSION2;
        Integer clientType = Constant.CLINT_TYPE;
        Long timestamp =  System.currentTimeMillis();

        specialParam.put("appId", appId);
        specialParam.put("version", version);
        specialParam.put("clientType", clientType);
        specialParam.put("timestamp", timestamp);

        StringBuilder sb = new StringBuilder();
        Set<String> keySet = specialParam.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = specialParam.get(key);
            sb.append(key).append("=").append(value).append("&");
        }
        String paramStr = "";
        if(sb.length() > 0) {
             paramStr = sb.substring(0, sb.length() - 1);
        }
        System.out.printf("请求参数(加密前):%s%n", paramStr);
        String params = XXTea.encrypt(paramStr,"UTF-8",ByteFormat.toHex(Constant.APP_SECRET.getBytes()));
        String signature = HmacSHATool.encodeHmacSHA256(appId+clientType+params+timestamp+version, Constant.APP_SECRET);
        // 返回全部参数
        Map<String,Object> resultMap = new HashMap<String, Object>();
        resultMap.put("appId", appId);
        resultMap.put("version", version);
        resultMap.put("clientType", clientType);
        resultMap.put("timestamp", timestamp);
        resultMap.put("signature", signature);
        resultMap.put("params", params);
        return resultMap;
    }
}
