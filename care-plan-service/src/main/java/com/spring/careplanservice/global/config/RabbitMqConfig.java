package com.spring.careplanservice.global.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String SCHEDULE_EXCHANGE = "schedule.exchange";

    public static final String SCHEDULE_COMPLETED_ROUTING_KEY = "schedule.completed.key";

    public static final String CARE_PLAN_SCHEDULE_COMPLETED_QUEUE = "care-plan.schedule-completed.queue";

    @Bean
    public DirectExchange scheduleExchange() {
        return new DirectExchange(SCHEDULE_EXCHANGE);
    }

    @Bean
    public Queue carePlanScheduleCompletedQueue() {
        return new Queue(
                CARE_PLAN_SCHEDULE_COMPLETED_QUEUE,
                true
        );
    }

    @Bean
    public Binding carePlanScheduleCompletedBinding(
            DirectExchange scheduleExchange,
            Queue carePlanScheduleCompletedQueue
    ) {
        return BindingBuilder
                .bind(carePlanScheduleCompletedQueue)
                .to(scheduleExchange)
                .with(SCHEDULE_COMPLETED_ROUTING_KEY);
    }
}
