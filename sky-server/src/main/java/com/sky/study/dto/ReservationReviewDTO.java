package com.sky.study.dto;

import lombok.Data;

@Data
public class ReservationReviewDTO {

    private Long reservationId;

    private Integer status;

    private String reviewRemark;
}
