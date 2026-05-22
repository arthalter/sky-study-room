package com.sky.study.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceSaveDTO {
    private Long id;
    // 资源编号，比如 A-101
    @NotBlank(message = "资源编号不能为空")
    private String resourceCode;

    // 资源名称，比如 1号静音座
    @NotBlank(message = "资源名称不能为空")
    private String resourceName;

    // 资源类型：PUBLIC_SEAT / PRIVATE_ROOM / MEETING_ROOM
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    // 楼层，比如 1F
    @NotBlank(message = "楼层不能为空")
    private String floor;

    // 开放时间，比如 08:00-22:00
    @NotBlank(message = "开放时间不能为空")
    private String openTime;

    // 描述
    private String description;
}
