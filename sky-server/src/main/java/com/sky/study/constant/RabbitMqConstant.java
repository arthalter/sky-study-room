package com.sky.study.constant;

public class RabbitMqConstant {

    public static final String RESERVATION_EXCHANGE = "reservation.exchange";
    public static final String RESERVATION_REVIEWED_QUEUE = "reservation.reviewed.queue";
    public static final String RESERVATION_REVIEWED_ROUTING_KEY = "reservation.reviewed";

    private RabbitMqConstant() {
    }
}
