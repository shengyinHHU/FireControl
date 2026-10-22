package com.cn21.open.demo.getMediaUrlByDeviceTree;

import cn.hutool.json.JSONUtil;
import com.cn21.open.demo.common.Api;
import com.cn21.open.demo.common.Constant;
import com.cn21.open.demo.common.Variable;
import com.cn21.open.demo.entity.LastRegionNode;
import com.cn21.open.demo.entity.Node;
import com.cn21.open.demo.entity.PageDto;

import java.util.*;

/**
 * @Author lyf15
 * @Description 查询我的设备（设备树），以及，获取设备的直播链接地址（标准RTSP,HLS,RTMP）
 * 参考文档：https://vcp.dlife.cn/portal/document-detail?group=1649110730734931970
 * 示例接口，若返回错误，请参考门户：天翼视联-能力开放门户Q&A，地址：https://vcp.dlife.cn/portal/QA
 * --直播地址，有效时间说明，expireTime未实际应用（待实现）
 * 国标/社标设备：
 *（1）未开通云存套餐，链接有效时间20秒（接口请求链接时，才会下触发云存开始拉流，响应不及时可能会出现流地址无法播放或者过期等问题）
 *（2）已开通云存套餐，链接有效时间5分钟（设备绑定了云存，云存会一直拉流，而不是在获取链接时才开始拉流）
 * 企标设备：
 * 链接有效时间10分钟（是否开通云存不影响有效时间）
 * 跨越问题，门户（天翼视联能力开放管理平台）-应用管理-开放配置-业务域名，配置合作方域名
 * @Date 2023/8/3110:45
 */
public class getMediaUrlByDeviceTree extends BaseDemo{
    /**
     * 能力开放访问令牌,先对接https://vcp.dlife.cn/portal/document中的调用凭证，获取访问令牌accessToken再测试以下相关接口
     */
    private static String accessToken = "you accessToken";
    /**
     * 测试设备，合作方修改成自己通过目录获取的设备，若报设备无权限，请在云眼上检查是否正确绑定/级联该设备到获取accessToken的云眼账号
     */
    private static String deviceCode = "you deviceCode";
    /**
     * 区域ID,请修改成获取的regionId{@link LastRegionNode#regionCode}
     * 通过{@link getMediaUrlByDeviceTree#getLastRegions(String)}获取
     */
    private static String regionCode = "320105002001001006";
    /**
     * 请对照文档，修改公共参数{@link Constant}并修改本demo的成员变量打开注释调用，
     * @param args
     */
    public static void main(String[] args) {
        //查询设备树行政目录结构,regionId区域id，为空查首层区域
        getRegions(accessToken,null);
        //获取设备树最后一层区域列表
        getLastRegions(accessToken);
        //根据区域码分页查询设备树下设备列表
        getDevicesByRegionCode(accessToken,regionCode);
        //获取标准直播流-获取设备RTSP直播链接地址
        getDeviceMediaUrlRtsp(accessToken,deviceCode);
        //转码直播视频流-获取HLS直播链接地址
        getDeviceMediaUrlHls(accessToken,deviceCode);
        //转码直播视频流-获取RTMP直播链接地址
        getDeviceMediaUrlRtmp(accessToken,deviceCode);
    }


    /**
     * 查询设备树行政目录结构
     * 设备树目录结构由行政区域编码组成的6级目录，设备信息在最后一级（第六级）
     * 此方法可递归,也只直接调{@link getMediaUrlByDeviceTree#getLastRegions(String)}获取最后一级行政区域
     * @param accessToken
     */
    private static List<Node> getRegions(String accessToken,String regionId) {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_REGIONS_URI);
        List<Node> nodeList =new  ArrayList();
        try {
            // 接口定义具体入参
            Map<String, Object> params = new HashMap<String, Object>(4);
            //用户无感知授权登录用户需要填写此企业主账号，其他类型登录用户不需要此参数
            params.put(Variable.ENTERPRISE_USER, Constant.ENTERPRISE_USER);
            //区域自增id，为空查首层区域
            Optional.ofNullable(regionId).ifPresent((r) -> params.put("regionId", regionId));
            params.put(Variable.ACCESS_TOKEN, accessToken);
            nodeList = getResultListBySendMessage(url, params, genTokenHeader(), Node.class);
        } catch (Exception e){
            e.printStackTrace();
        }
        return nodeList;
    }

    /**
     * 获取设备树最后一层区域列表
     * @param accessToken
     */
    private static List<LastRegionNode> getLastRegions(String accessToken) {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_LAST_REGIONS_URI);
        List<LastRegionNode> nodeList =new  ArrayList();
        try {
            // 接口定义具体入参
            Map<String, Object> params = new HashMap<String, Object>(4);
            //用户无感知授权登录用户需要填写此企业主账号，其他类型登录用户不需要此参数
            params.put(Variable.ENTERPRISE_USER, Constant.ENTERPRISE_USER);
            params.put(Variable.ACCESS_TOKEN, accessToken);
            nodeList = getResultListBySendMessage(url, params, genTokenHeader(), LastRegionNode.class);
        } catch (Exception e){
            e.printStackTrace();
        }
        return nodeList;
    }
    /**
     *根据区域码分页查询设备树下设备列表
     * @param accessToken
     * @param regionCode 最后一级的行政区域编码,由{@link getMediaUrlByDeviceTree#getLastRegions(String)}或{@link getMediaUrlByDeviceTree#getRegions(String)}获取
     */
    private static PageDto getDevicesByRegionCode(String accessToken,String regionCode) {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_DEVICES_BY_REGION_CODE_URI);
        PageDto pageDto = new PageDto();
        try {
            // 接口定义具体入参
            Map<String, Object> params = new HashMap<String, Object>(4);
            //用户无感知授权登录用户需要填写此企业主账号，其他类型登录用户不需要此参数
            params.put(Variable.ENTERPRISE_USER, Constant.ENTERPRISE_USER);
            params.put(Variable.ACCESS_TOKEN, accessToken);
            params.put(Variable.PAGE_NO, 1);
            params.put(Variable.PAGE_SIZE, 10);
            params.put(Variable.REGION_CODE, regionCode);
            pageDto = getResultVoBySendMessage(url, params, genTokenHeader(), PageDto.class);
        } catch (Exception e){
            e.printStackTrace();
        }
        return pageDto;
    }

    /**
     * 获取标准直播流-获取标准RTSP直播链接地址
     * @param accessToken
     * @param deviceCode
     */
    private static void getDeviceMediaUrlRtsp(String accessToken,String deviceCode) {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_DEVICE_MEDIA_URL_RTSP_URI);
        try{
            Map<String, Object> params = new HashMap<>(8);
            params.put(Variable.ACCESS_TOKEN,accessToken);
            //用户无感知授权登录用户需要填写此企业主账号，其他类型登录用户不需要此参数
            params.put(Variable.ENTERPRISE_USER, Constant.ENTERPRISE_USER);
            params.put(Variable.DEVICE_CODE,deviceCode);
            //直播类型（1.标清；0.高清）
            params.put(Variable.MEDIA_TYPE,0);
            //	静音标识（0.非静音;1.静音）默认为0
            params.put(Variable.MUTE,0);
            String resultStr = sendMessage(url, params, genTokenHeader());
            Object resultData = JSONUtil.parseObj(resultStr).get("data");
            Optional.ofNullable(resultData).ifPresent((da) ->{
                String data = da.toString();
                //解密,若公共请求参数中version为v1.0，用appSecret XXTea解密，若公共请求参数中version为1.1，用RSA私钥解密
                String mediaUrl = null;
                try {
                    mediaUrl = getDecryptData(data,Constant.VERSION);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("获取标准RTSP直播链接地址为："+mediaUrl);
            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 转码直播视频流-获取HLS直播链接地址（支持m3u8）
     * 国标设备不支持supportDomain参数，若获取的是https直播地址可以自行去掉s转为http，http无法转为https
     * @param accessToken
     * @param deviceCode
     */
    private static void getDeviceMediaUrlHls(String accessToken,String deviceCode) {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_DEVICE_MEDIA_URL_HLS_URI);
        try {
            Map<String, Object> params = new HashMap<>(8);
            params.put(Variable.ACCESS_TOKEN, accessToken);
            //用户无感知授权登录用户需要填写此企业主账号，其他类型登录用户不需要此参数
            params.put(Variable.ENTERPRISE_USER, Constant.ENTERPRISE_USER);
            params.put(Variable.DEVICE_CODE, deviceCode);
            //直播类型（1.标清；0.高清）
            params.put(Variable.MEDIA_TYPE, 0);
            //	获取https / http 协议 （0: http ;1:https）默认为0
            params.put(Variable.SUPPORT_DOMAIN, 1);
            //	静音标识（0.非静音;1.静音）默认为0
            params.put(Variable.MUTE, 0);
            String resultStr = sendMessage(url, params, genTokenHeader());
            Object resultData = JSONUtil.parseObj(resultStr).get("data");
            Optional.ofNullable(resultData).ifPresent((da) ->{
                String data = da.toString();
                //解密,若公共请求参数中version为v1.0，用appSecret XXTea解密，若公共请求参数中version为1.1，用RSA私钥解密
                String mediaUrl = null;
                try {
                    mediaUrl = getDecryptData(data,Constant.VERSION);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("获取HLS直播链接地址为：" + mediaUrl);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 转码直播视频流-获取RTMP直播链接地址
     * @param accessToken
     * @param deviceCode
     */
    private static void getDeviceMediaUrlRtmp(String accessToken,String deviceCode) {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_DEVICE_MEDIA_URL_RTMP_URI);
        try {
            Map<String, Object> params = new HashMap<>(8);
            params.put(Variable.ACCESS_TOKEN, accessToken);
            //用户无感知授权登录用户需要填写此企业主账号，其他类型登录用户不需要此参数
            params.put(Variable.ENTERPRISE_USER, Constant.ENTERPRISE_USER);
            params.put(Variable.DEVICE_CODE, deviceCode);
            //直播类型（1.标清；0.高清）
            params.put(Variable.MEDIA_TYPE, 0);
            //	静音标识（0.非静音;1.静音）默认为0
            params.put(Variable.MUTE, 0);
            String resultStr = sendMessage(url, params, genTokenHeader());
            Object resultData = JSONUtil.parseObj(resultStr).get("data");
            Optional.ofNullable(resultData).ifPresent((da) ->{
                String data = da.toString();
                //解密,若公共请求参数中version为v1.0，用appSecret XXTea解密，若公共请求参数中version为1.1，用RSA私钥解密
                String mediaUrl = null;
                try {
                    mediaUrl = getDecryptData(data,Constant.VERSION);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("获取标准RTMP直播链接地址为：" + mediaUrl);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
