package com.sky.study.service;

import com.sky.study.dto.NotificationPageQueryDTO;
import com.sky.study.vo.PageResult;

public interface NotificationService {

    PageResult pageQuery(NotificationPageQueryDTO notificationPageQueryDTO);
}
