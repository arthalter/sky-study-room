package com.sky.study.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Integer readStatus;

    private LocalDateTime createTime;
}
