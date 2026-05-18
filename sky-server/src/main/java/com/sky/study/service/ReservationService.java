package com.sky.study.service;

import com.sky.study.dto.ReservationPageQueryDTO;
import com.sky.study.dto.ReservationReviewDTO;
import com.sky.study.dto.ReservationSubmitDTO;
import com.sky.study.vo.PageResult;

public interface ReservationService {

    void submit(ReservationSubmitDTO reservationSubmitDTO);

    PageResult pageQuery(ReservationPageQueryDTO reservationPageQueryDTO);

    PageResult adminPageQuery(ReservationPageQueryDTO reservationPageQueryDTO);

    void review(ReservationReviewDTO reservationReviewDTO);

    void cancel(Long id);
}
