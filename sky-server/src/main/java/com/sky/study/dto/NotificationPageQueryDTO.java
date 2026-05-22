package com.sky.study.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class NotificationPageQueryDTO {

    @Min(value = 1, message = "页码必须大于0")
    private Integer page;

    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize;

    private Integer readStatus;
}
