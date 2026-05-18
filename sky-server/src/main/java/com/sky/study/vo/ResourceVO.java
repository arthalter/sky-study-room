package com.sky.study.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceVO {

    private Long id;

    private String resourceCode;

    private String resourceName;

    private String resourceType;

    private Integer status;

    private String floor;

    private String openTime;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}