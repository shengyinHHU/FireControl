package com.cn21.open.demo.entity;

import lombok.Data;

/**
 * @Author lyf15
 * @Description getLastRegions响应实体类
 * @Date 2023/9/617:54
 */
@Data
public class LastRegionNode {
    /**
     *区域自增id
     */
    private Long id;
    /**
     *区域编码
     */
    private String regionCode;
    /**
     *区域名称
     */
    private String name;
    /**
     *	层级
     */
    private Integer level;
    /**
     *是否有子节点
     */
    private Boolean hasChildren;
    /**
     *该区域下所有设备数量
     */
    private Integer deviceCount;
}
