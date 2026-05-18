package com.sky.study.dto;

import lombok.Data;

@Data
public class ResourcePageQueryDTO {

    /**
     * 页码
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 状态
     */
    private Integer status;
}