package com.cn21.open.demo.getAccessTokenByPlatform;

import cn.hutool.json.JSONUtil;
import com.cn21.open.demo.common.Api;
import com.cn21.open.demo.common.Constant;
import com.cn21.open.demo.common.Variable;
import com.cn21.open.demo.entity.AccessTokenVo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Author lyf15
 * @Description 用户无感知获取令牌-平台应用获取访问令牌测试demo
 * 参考文档：https://vcp.dlife.cn/portal/document-detail?group=1665564929510465537
 * 1、平台应用在访问接口前，必须通过门户中“应用管理-成员管理-企业主授权信息”模块添加企业主。
 * 2、平台应用未打开企业主免授权的场景下，并且要求企业主通过下发的授权提醒“短信”或“邮件”点开授权界面完成授权行为。
 * 3、平台应用打开企业主免授权的场景，需要由平台应用所有单位上传“企业主免授权”申请函件加盖单位公章，代替设备所有者（企业主）授权，此时企业主将不会收到授权提醒“短信”或“邮件”。
 * 4、以上两种场景添加企业主成功后，请求能力开放服务接口时，必须携带 enterpriseUser 参数（授权通过的企业主手机号）查询设备清单、拉取设备视频等能力。否则报：30045，企业主应用授权为空
 * 若有其他问题，请参考门户（示例了常见问题）：天翼视联-能力开放门户Q&A，地址：https://vcp.dlife.cn/portal/QA
 * @Date 2023/7/1415:22
 */
public class PlatformDemo extends BaseDemo {
    /**
     * 请合作方置换为获取的访问令牌accessToken
     */
    private static String accessToken = "you accessToken";
    /**
     * 请合作方置换为获取的刷新令牌refreshToken
     */
    private static String refreshToken = "you refreshToken";
    /**
     * 请对照文档，修改公共参数{@link Constant}并修改本demo的成员变量打开注释调用，
     * @param args
     */
    public static void main(String[] args) {
        //获取访问令牌accessToken
        getAccessToken();
        //刷新token
        getAccessTokenByRefreshToken(refreshToken);
        //定时刷新accessToken实现保活机制简单示例
        refreshTokenByScheduled();
    }

    /**
     * 平台应用，获取能力开放的accessToken
     * @param
     */
    private static AccessTokenVo getAccessToken() {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_ACCESS_TOKEN_URI);
        AccessTokenVo accessTokenVo = new AccessTokenVo();
        try{
            Map<String, Object> params = new HashMap<>(2);
            //根据获取令牌或者刷新令牌场景选择以下两个值之一：
            //"vcp_189"：平台应用获取访问令牌
            //"refresh_token"：平台应用刷新令牌
            params.put(Variable.GRANT_TYPE,"vcp_189");
            accessTokenVo = getResultVoBySendMessage(url, params, genTokenHeader(), AccessTokenVo.class);
        } catch (Exception e){
            e.printStackTrace();
        }
        return accessTokenVo;
    }
    /**
     * 通过getAccessToken接口刷新访问令牌，合作方可通过此接口实现自己的令牌保活机制
     * 平台应用accessToke有消息为7天，refreshToken有效期为30天，通过此接口，可获取一个全新生命周期的（新的）accessToken和refreshToken
     * @param refreshToken
     */
    private static AccessTokenVo getAccessTokenByRefreshToken(String refreshToken) {
        String url = getUrl(Constant.PRO_DOMAIN, Api.GET_ACCESS_TOKEN_URI);
        AccessTokenVo accessTokenVo = new AccessTokenVo();
        try{
            Map<String, Object> params = new HashMap<>(2);
            //"refresh_token"： 标准应用刷新令牌
            params.put(Variable.GRANT_TYPE,"refresh_token");
            //通过getAccessToken接口获取的refreshToken)
            params.put(Variable.REFRESH_TOKEN,refreshToken);
            accessTokenVo = getResultVoBySendMessage(url, params, genTokenHeader(), AccessTokenVo.class);
        } catch (Exception e){
            e.printStackTrace();
        }
        return accessTokenVo;
    }
    /**
     * 合作方可通过此接口实现自己的令牌保活机制示例
     * 此方法只是简单做一个示例，具体实现方式自行斟酌，主要是通过定时任务之类的定期刷新缓存从而实现保活
     */
    private static void refreshTokenByScheduled(){
        //1天后开始执行，1天刷新一次，平台应用refreshToken有效期是30天，时间长度自行斟酌
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(() -> {
                    //todo 从缓存（数据库）之类存储内的获取refreshToken，建议关联企业主账号，保障唯一主键关联
                    getAccessTokenByRefreshToken(refreshToken);
                    //todo 保存/刷新应用信息，accessToken到缓存（数据库）
                },1,1,TimeUnit.DAYS);
    }
}
