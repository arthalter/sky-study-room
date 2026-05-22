package com.sky.study.controller.user;

import com.sky.study.dto.NotificationPageQueryDTO;
import com.sky.study.service.NotificationService;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam Integer page,
                                   @RequestParam Integer pageSize,
                                   @RequestParam(required = false) Integer readStatus) {
        NotificationPageQueryDTO queryDTO = new NotificationPageQueryDTO();
        queryDTO.setPage(page);
        queryDTO.setPageSize(pageSize);
        queryDTO.setReadStatus(readStatus);
        return Result.success(notificationService.pageQuery(queryDTO));
    }
}
