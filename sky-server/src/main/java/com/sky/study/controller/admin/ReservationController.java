package com.sky.study.controller.admin;

import com.sky.study.dto.ReservationPageQueryDTO;
import com.sky.study.dto.ReservationReviewDTO;
import com.sky.study.service.ReservationService;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminReservationController")
@RequestMapping("/api/admin/reservation")
@Slf4j
public class ReservationController {

    @Resource
    private ReservationService reservationService;

    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam Integer page,
                                   @RequestParam Integer pageSize,
                                   @RequestParam(required = false) Integer status) {
        ReservationPageQueryDTO reservationPageQueryDTO = new ReservationPageQueryDTO();
        reservationPageQueryDTO.setPage(page);
        reservationPageQueryDTO.setPageSize(pageSize);
        reservationPageQueryDTO.setStatus(status);
        return Result.success(reservationService.adminPageQuery(reservationPageQueryDTO));
    }

    @PostMapping("/review")
    public Result review(@RequestBody ReservationReviewDTO reservationReviewDTO) {
        log.info("reservation review request: {}", reservationReviewDTO);
        reservationService.review(reservationReviewDTO);
        return Result.success();
    }
}
