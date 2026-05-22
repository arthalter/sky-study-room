package com.sky.study.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservationSubmitDTO {

    @NotNull(message = "资源不能为空")
    private Long resourceId;

    @NotNull(message = "预约日期不能为空")
    @FutureOrPresent(message = "预约日期不能早于今天")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reserveDate;

    @NotNull(message = "预约开始时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "预约结束时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotBlank(message = "预约用途不能为空")
    private String purpose;
}
