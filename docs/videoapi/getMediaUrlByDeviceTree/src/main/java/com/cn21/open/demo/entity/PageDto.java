package com.cn21.open.demo.entity;

import lombok.Data;

import java.util.List;

/**
 * @Author lyf15
 * @Description
 * @Date 2023/9/618:00
 */
@Data
public class PageDto {
    /**
     *当前页码，从1开始
     */
    private Integer pageNo;
    /**
     *每页显示条数
     */
    private Integer pageSize;
    /**
     *数据总条数
     */
    private Integer total;
    /**
     *
     */
    private List<String> deviceCodeList;
}
