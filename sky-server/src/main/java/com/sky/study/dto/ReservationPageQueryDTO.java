package com.sky.study.dto;

import lombok.Data;

@Data
public class ReservationPageQueryDTO {

    private Integer page;

    private Integer pageSize;

    private Integer status;
}
