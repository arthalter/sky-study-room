package com.sky.study.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class Reservation {

    private Long id;

    private Long userId;

    private Long resourceId;

    private LocalDate reserveDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String purpose;

    private Integer status;

    private String reviewRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
