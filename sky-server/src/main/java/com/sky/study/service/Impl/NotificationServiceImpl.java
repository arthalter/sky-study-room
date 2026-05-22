package com.sky.study.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.study.context.BaseContext;
import com.sky.study.dto.NotificationPageQueryDTO;
import com.sky.study.mapper.NotificationMapper;
import com.sky.study.service.NotificationService;
import com.sky.study.vo.NotificationVO;
import com.sky.study.vo.PageResult;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public PageResult pageQuery(NotificationPageQueryDTO notificationPageQueryDTO) {
        PageHelper.startPage(notificationPageQueryDTO.getPage(), notificationPageQueryDTO.getPageSize());
        Page<NotificationVO> page = (Page<NotificationVO>) notificationMapper.pageQueryByUserId(
                BaseContext.getCurrentId(),
                notificationPageQueryDTO.getReadStatus()
        );
        return new PageResult(page.getTotal(), page.getResult());
    }
}
