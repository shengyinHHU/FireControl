package com.cn21.open.demo.entity;

import lombok.Data;

/**
 * @Author lyf15
 * @Description
 * @Date 2023/9/610:57
 */
@Data
public class AccessTokenVo {
    /**
     * token值
     */
    private String accessToken;
    /**
     *刷新accessToken需传递
     */
    private String refreshToken;
    /**
     *token过期时间
     */
    private Integer expiresIn;
    /**
     *	refrshToken过期时间
     */
    private Integer refreshExpiresIn;
}
