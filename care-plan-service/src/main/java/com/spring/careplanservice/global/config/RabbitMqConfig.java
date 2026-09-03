package com.spring.careplanservice.global.config;


import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String CARE_PLAN_CONFIRMED_EXCHANGE = "care-plan.exchange";

    public static final String CARE_PLAN_CONFIRMED_ROUTING_KEY = "care-plan.confirmed";

    public static final String CARE_PLAN_COMPLETED_QUEUE = "care-plan.completed.queue";

    @Bean
    public DirectExchange carePlanExchange() {
        return new DirectExchange(
                CARE_PLAN_CONFIRMED_EXCHANGE
        );
    }

    @Bean
    public Queue carePlanCompletedQueue() {
        return new Queue(
                CARE_PLAN_COMPLETED_QUEUE,
                true
        );
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
