package com.sky.study.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ReservationVO {

    private Long id;

    private Long resourceId;

    private String resourceName;

    private String username;

    private LocalDate reserveDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String purpose;

    private Integer status;

    private String reviewRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
