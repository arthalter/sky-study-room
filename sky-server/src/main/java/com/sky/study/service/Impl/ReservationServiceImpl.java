package com.sky.study.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.study.constant.MessageConstant;
import com.sky.study.constant.ReservationStatusConstant;
import com.sky.study.constant.ResourceStatusConstant;
import com.sky.study.context.BaseContext;
import com.sky.study.dto.ReservationPageQueryDTO;
import com.sky.study.dto.ReservationReviewDTO;
import com.sky.study.dto.ReservationSubmitDTO;
import com.sky.study.entity.Reservation;
import com.sky.study.entity.ReservationAuditLog;
import com.sky.study.exception.BaseException;
import com.sky.study.mapper.ReservationAuditLogMapper;
import com.sky.study.mapper.ReservationMapper;
import com.sky.study.mapper.ResourceMapper;
import com.sky.study.mq.ReservationReviewMessagePublisher;
import com.sky.study.mq.message.ReservationReviewedMessage;
import com.sky.study.service.ReservationService;
import com.sky.study.utils.ReservationValidationUtil;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.ReservationVO;
import com.sky.study.vo.ResourceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final ResourceMapper resourceMapper;
    private final ReservationAuditLogMapper reservationAuditLogMapper;
    private final ReservationReviewMessagePublisher reservationReviewMessagePublisher;

    public ReservationServiceImpl(ReservationMapper reservationMapper,
                                  ResourceMapper resourceMapper,
                                  ReservationAuditLogMapper reservationAuditLogMapper,
                                  ReservationReviewMessagePublisher reservationReviewMessagePublisher) {
        this.reservationMapper = reservationMapper;
        this.resourceMapper = resourceMapper;
        this.reservationAuditLogMapper = reservationAuditLogMapper;
        this.reservationReviewMessagePublisher = reservationReviewMessagePublisher;
    }

    @Override
    @Transactional
    public void submit(ReservationSubmitDTO reservationSubmitDTO) {
        if (reservationSubmitDTO.getResourceId() == null) {
            throw new BaseException(MessageConstant.RESOURCE_NOT_FOUND);
        }
        if (reservationSubmitDTO.getReserveDate() == null
                || reservationSubmitDTO.getStartTime() == null
                || reservationSubmitDTO.getEndTime() == null) {
            throw new BaseException("预约日期和时间不能为空");
        }
        if (reservationSubmitDTO.getReserveDate().isBefore(LocalDate.now())) {
            throw new BaseException("预约日期不能早于今天");
        }
        ReservationValidationUtil.validateTimeRange(reservationSubmitDTO.getStartTime(), reservationSubmitDTO.getEndTime());

        ResourceVO resourceVO = resourceMapper.getById(reservationSubmitDTO.getResourceId());
        if (resourceVO == null) {
            throw new BaseException(MessageConstant.RESOURCE_NOT_FOUND);
        }
        if (!ResourceStatusConstant.ENABLED.equals(resourceVO.getStatus())) {
            throw new BaseException(MessageConstant.RESOURCE_UNAVAILABLE);
        }
        ReservationValidationUtil.validateWithinOpenTime(
                reservationSubmitDTO.getStartTime(),
                reservationSubmitDTO.getEndTime(),
                resourceVO.getOpenTime()
        );

        Integer conflictCount = reservationMapper.countApprovedConflict(
                reservationSubmitDTO.getResourceId(),
                reservationSubmitDTO.getReserveDate(),
                reservationSubmitDTO.getStartTime(),
                reservationSubmitDTO.getEndTime()
        );
        if (conflictCount != null && conflictCount > 0) {
            throw new BaseException(MessageConstant.RESERVATION_CONFLICT);
        }

        Reservation reservation = new Reservation();
        BeanUtils.copyProperties(reservationSubmitDTO, reservation);
        reservation.setUserId(BaseContext.getCurrentId());
        reservation.setStatus(ReservationStatusConstant.PENDING);
        reservationMapper.insert(reservation);

        log.info("reservation submit success: userId={}, resourceId={}",
                reservation.getUserId(), reservation.getResourceId());
    }

    @Override
    public PageResult pageQuery(ReservationPageQueryDTO reservationPageQueryDTO) {
        Long userId = BaseContext.getCurrentId();
        PageHelper.startPage(reservationPageQueryDTO.getPage(), reservationPageQueryDTO.getPageSize());
        Page<ReservationVO> page = (Page<ReservationVO>) reservationMapper.pageQueryByUserId(
                userId,
                reservationPageQueryDTO.getStatus()
        );
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public PageResult adminPageQuery(ReservationPageQueryDTO reservationPageQueryDTO) {
        PageHelper.startPage(reservationPageQueryDTO.getPage(), reservationPageQueryDTO.getPageSize());
        Page<ReservationVO> page = (Page<ReservationVO>) reservationMapper.pageQuery(
                reservationPageQueryDTO.getStatus()
        );
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void review(ReservationReviewDTO reservationReviewDTO) {
        Reservation reservation = reservationMapper.getByIdForUpdate(reservationReviewDTO.getReservationId());
        if (reservation == null) {
            throw new BaseException(MessageConstant.RESERVATION_NOT_FOUND);
        }
        validateReviewStatusTransition(reservation.getStatus(), reservationReviewDTO.getStatus());

        if (ReservationStatusConstant.APPROVED.equals(reservationReviewDTO.getStatus())) {
            reservationMapper.lockApprovedByResourceAndDate(
                    reservation.getResourceId(),
                    reservation.getReserveDate()
            );
            Integer conflictCount = reservationMapper.countApprovedConflict(
                    reservation.getResourceId(),
                    reservation.getReserveDate(),
                    reservation.getStartTime(),
                    reservation.getEndTime()
            );
            if (conflictCount != null && conflictCount > 0) {
                throw new BaseException(MessageConstant.RESERVATION_CONFLICT);
            }
        }

        Reservation updateReservation = new Reservation();
        updateReservation.setId(reservationReviewDTO.getReservationId());
        updateReservation.setStatus(reservationReviewDTO.getStatus());
        updateReservation.setReviewRemark(reservationReviewDTO.getReviewRemark());
        reservationMapper.update(updateReservation);
        writeAuditLog(reservation, reservationReviewDTO);
        publishReviewMessage(reservation, reservationReviewDTO);
    }

    @Override
    public void cancel(Long id) {
        Reservation reservation = reservationMapper.getById(id);
        if (reservation == null) {
            throw new BaseException(MessageConstant.RESERVATION_NOT_FOUND);
        }
        if (!BaseContext.getCurrentId().equals(reservation.getUserId())) {
            throw new BaseException(MessageConstant.NO_PERMISSION);
        }
        validateCancelStatusTransition(reservation.getStatus());

        Reservation updateReservation = new Reservation();
        updateReservation.setId(id);
        updateReservation.setStatus(ReservationStatusConstant.CANCELED);
        reservationMapper.update(updateReservation);
    }

    private void validateReviewStatusTransition(Integer currentStatus, Integer targetStatus) {
        if (!ReservationStatusConstant.PENDING.equals(currentStatus)) {
            throw new BaseException(MessageConstant.RESERVATION_STATUS_ERROR);
        }
        if (!ReservationStatusConstant.APPROVED.equals(targetStatus)
                && !ReservationStatusConstant.REJECTED.equals(targetStatus)) {
            throw new BaseException(MessageConstant.RESERVATION_STATUS_ERROR);
        }
    }

    private void validateCancelStatusTransition(Integer currentStatus) {
        if (ReservationStatusConstant.CANCELED.equals(currentStatus)
                || ReservationStatusConstant.REJECTED.equals(currentStatus)) {
            throw new BaseException(MessageConstant.RESERVATION_STATUS_ERROR);
        }
    }

    private void writeAuditLog(Reservation reservation, ReservationReviewDTO reservationReviewDTO) {
        ReservationAuditLog auditLog = new ReservationAuditLog();
        auditLog.setReservationId(reservation.getId());
        auditLog.setAdminId(BaseContext.getCurrentId());
        auditLog.setOldStatus(reservation.getStatus());
        auditLog.setNewStatus(reservationReviewDTO.getStatus());
        auditLog.setReviewRemark(reservationReviewDTO.getReviewRemark());
        reservationAuditLogMapper.insert(auditLog);
    }

    private void publishReviewMessage(Reservation reservation, ReservationReviewDTO reservationReviewDTO) {
        ReservationReviewedMessage message = new ReservationReviewedMessage();
        message.setReservationId(reservation.getId());
        message.setUserId(reservation.getUserId());
        message.setStatus(reservationReviewDTO.getStatus());
        message.setReviewRemark(reservationReviewDTO.getReviewRemark());
        reservationReviewMessagePublisher.publishAfterCommit(message);
    }

}
