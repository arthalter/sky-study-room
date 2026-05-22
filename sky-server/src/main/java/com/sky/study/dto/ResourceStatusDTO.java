package com.sky.study.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceStatusDTO {
    @NotNull(message = "资源不能为空")
    private Long id;

    @NotNull(message = "资源状态不能为空")
    @Min(value = 0, message = "资源状态错误")
    @Max(value = 2, message = "资源状态错误")
    private Integer status;
}
