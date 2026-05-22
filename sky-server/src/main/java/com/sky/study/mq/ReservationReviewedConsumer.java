package com.sky.study.mq;

import com.sky.study.constant.NotificationReadStatusConstant;
import com.sky.study.constant.RabbitMqConstant;
import com.sky.study.constant.ReservationStatusConstant;
import com.sky.study.entity.Notification;
import com.sky.study.mapper.NotificationMapper;
import com.sky.study.mq.message.ReservationReviewedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReservationReviewedConsumer {

    private final NotificationMapper notificationMapper;

    public ReservationReviewedConsumer(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @RabbitListener(queues = RabbitMqConstant.RESERVATION_REVIEWED_QUEUE)
    public void handle(ReservationReviewedMessage message) {
        Notification notification = new Notification();
        notification.setUserId(message.getUserId());
        notification.setReadStatus(NotificationReadStatusConstant.UNREAD);
        notification.setTitle(buildTitle(message.getStatus()));
        notification.setContent(buildContent(message));
        notificationMapper.insert(notification);
        log.info("reservation reviewed notification created: reservationId={}, userId={}",
                message.getReservationId(), message.getUserId());
    }

    private String buildTitle(Integer status) {
        if (ReservationStatusConstant.APPROVED.equals(status)) {
            return "预约审核通过";
        }
        if (ReservationStatusConstant.REJECTED.equals(status)) {
            return "预约审核未通过";
        }
        return "预约审核结果更新";
    }

    private String buildContent(ReservationReviewedMessage message) {
        String result = ReservationStatusConstant.APPROVED.equals(message.getStatus()) ? "已通过" : "未通过";
        String remark = message.getReviewRemark();
        if (remark == null || remark.isBlank()) {
            return "您的预约申请" + result + "。";
        }
        return "您的预约申请" + result + "，备注：" + remark;
    }
}
