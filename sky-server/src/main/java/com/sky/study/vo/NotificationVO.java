package com.sky.study.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;

    private String title;

    private String content;

    private Integer readStatus;

    private LocalDateTime createTime;
}
