package com.sky.study.dto;

import lombok.Data;

@Data
public class ResourceSaveDTO {
    private Long id;
    // 资源编号，比如 A-101
    private String resourceCode;

    // 资源名称，比如 1号静音座
    private String resourceName;

    // 资源类型：PUBLIC_SEAT / PRIVATE_ROOM / MEETING_ROOM
    private String resourceType;

    // 楼层，比如 1F
    private String floor;

    // 开放时间，比如 08:00-22:00
    private String openTime;

    // 描述
    private String description;
}