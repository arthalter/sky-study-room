package com.sky.study.controller.user;

import com.sky.study.dto.ReservationPageQueryDTO;
import com.sky.study.dto.ReservationSubmitDTO;
import com.sky.study.service.ReservationService;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("userReservationController")
@RequestMapping("/api/user/reservation")
@Slf4j
public class ReservationController {

    @Resource
    private ReservationService reservationService;

    @PostMapping("/submit")
    public Result submit(@RequestBody @Valid ReservationSubmitDTO reservationSubmitDTO) {
        log.info("reservation submit request: {}", reservationSubmitDTO);
        reservationService.submit(reservationSubmitDTO);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam Integer page,
                                   @RequestParam Integer pageSize,
                                   @RequestParam(required = false) Integer status) {
        ReservationPageQueryDTO reservationPageQueryDTO = new ReservationPageQueryDTO();
        reservationPageQueryDTO.setPage(page);
        reservationPageQueryDTO.setPageSize(pageSize);
        reservationPageQueryDTO.setStatus(status);
        return Result.success(reservationService.pageQuery(reservationPageQueryDTO));
    }

    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return Result.success();
    }
}
