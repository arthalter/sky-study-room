package com.sky.study.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationAuditLog {

    private Long id;

    private Long reservationId;

    private Long adminId;

    private Integer oldStatus;

    private Integer newStatus;

    private String reviewRemark;

    private LocalDateTime createTime;
}
