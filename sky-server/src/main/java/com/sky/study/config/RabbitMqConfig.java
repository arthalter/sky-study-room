package com.sky.study.config;

import com.sky.study.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange reservationExchange() {
        return new TopicExchange(RabbitMqConstant.RESERVATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue reservationReviewedQueue() {
        return new Queue(RabbitMqConstant.RESERVATION_REVIEWED_QUEUE, true);
    }

    @Bean
    public Binding reservationReviewedBinding(Queue reservationReviewedQueue, TopicExchange reservationExchange) {
        return BindingBuilder.bind(reservationReviewedQueue)
                .to(reservationExchange)
                .with(RabbitMqConstant.RESERVATION_REVIEWED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
