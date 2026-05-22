package com.sky.study.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationReviewDTO {

    @NotNull(message = "预约不能为空")
    private Long reservationId;

    @NotNull(message = "审核状态不能为空")
    @Min(value = 2, message = "审核状态错误")
    @Max(value = 3, message = "审核状态错误")
    private Integer status;

    private String reviewRemark;
}
