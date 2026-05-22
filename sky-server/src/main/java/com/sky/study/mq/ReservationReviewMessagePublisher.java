package com.sky.study.mq;

import com.sky.study.constant.RabbitMqConstant;
import com.sky.study.mq.message.ReservationReviewedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
public class ReservationReviewMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReservationReviewMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAfterCommit(ReservationReviewedMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(message);
                }
            });
            return;
        }
        publish(message);
    }

    private void publish(ReservationReviewedMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConstant.RESERVATION_EXCHANGE,
                    RabbitMqConstant.RESERVATION_REVIEWED_ROUTING_KEY,
                    message
            );
            log.info("reservation reviewed message sent: reservationId={}, userId={}",
                    message.getReservationId(), message.getUserId());
        } catch (AmqpException e) {
            log.warn("reservation reviewed message send failed: reservationId={}",
                    message.getReservationId(), e);
        }
    }
}
