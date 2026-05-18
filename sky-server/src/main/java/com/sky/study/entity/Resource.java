package com.sky.study.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Resource {

    private Long id;

    private String resourceCode;

    private String resourceName;

    private String resourceType;

    /**
     * 资源状态：1-可用 0-停用 2-维修
     */
    private Integer status;

    private String floor;

    private String openTime;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}