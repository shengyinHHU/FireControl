package com.cn21.open.demo.entity;

import lombok.Data;

/**
 * @Author lyf15
 * @Description getRegions响应实体类
 * @Date 2023/9/617:47
 */
@Data
public class Node {
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
     *层级
     */
    private Integer level;
    /**
     *	是否有子节点
     */
    private Boolean hasChildren;
    /**
     *父节点自增id
     */
    private Integer pid;
    /**
     *父节点区域编码
     */
    private String pRegionCode;
    /**
     *	区域类型：1 用户创建 2 导入 3 管理后台用户录入
     */
    private Integer regionType;
}
