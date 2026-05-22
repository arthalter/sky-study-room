package com.sky.study.mq.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReservationReviewedMessage implements Serializable {

    private Long reservationId;

    private Long userId;

    private Integer status;

    private String reviewRemark;
}
