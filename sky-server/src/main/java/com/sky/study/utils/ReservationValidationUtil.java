package com.sky.study.utils;

import com.sky.study.exception.BaseException;

import java.time.LocalTime;

public class ReservationValidationUtil {

    private static final String OPEN_TIME_SEPARATOR = "-";

    private ReservationValidationUtil() {
    }

    public static void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BaseException("预约开始时间必须早于结束时间");
        }
    }

    public static void validateWithinOpenTime(LocalTime startTime, LocalTime endTime, String openTime) {
        String[] parts = openTime == null ? new String[0] : openTime.split(OPEN_TIME_SEPARATOR);
        if (parts.length != 2) {
            throw new BaseException("资源开放时间配置错误");
        }

        LocalTime openStart = LocalTime.parse(parts[0].trim());
        LocalTime openEnd = LocalTime.parse(parts[1].trim());
        if (startTime.isBefore(openStart) || endTime.isAfter(openEnd)) {
            throw new BaseException("预约时间不在资源开放时间范围内");
        }
    }
}
