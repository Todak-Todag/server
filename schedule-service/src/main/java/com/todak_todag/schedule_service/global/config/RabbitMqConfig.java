package com.todak_todag.schedule_service.global.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // 공통 Exchange
    public static final String SCHEDULE_EXCHANGE = "schedule.exchange";

    // CarePlanCompleted
    public static final String CARE_PLAN_COMPLETED_ROUTING_KEY = "schedule.completed.key";
    public static final String CARE_PLAN_SCHEDULE_COMPLETED_QUEUE = "care-plan.schedule-completed.queue";

    // ProviderReMatched
    public static final String PROVIDER_RE_MATCHED_ROUTING_KEY = "schedule.rematched.key";
    public static final String PROVIDER_SCHEDULE_REMATCHED_QUEUE = "provider.schedule-rematched.queue";

    @Bean
    public DirectExchange scheduleExchange() {
        return new DirectExchange(SCHEDULE_EXCHANGE);
    }

    @Bean
    public Queue carePlanScheduleCompletedQueue() {
        return new Queue(CARE_PLAN_SCHEDULE_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding carePlanScheduleCompletedBinding(
            DirectExchange scheduleExchange,
            Queue carePlanScheduleCompletedQueue
    ) {
        return BindingBuilder.bind(carePlanScheduleCompletedQueue)
                .to(scheduleExchange)
                .with(CARE_PLAN_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Queue providerScheduleRematchedQueue() {
        return new Queue(PROVIDER_SCHEDULE_REMATCHED_QUEUE, true);
    }

    @Bean
    public Binding providerScheduleRematchedBinding(
            DirectExchange scheduleExchange,
            Queue providerScheduleRematchedQueue
    ) {
        return BindingBuilder.bind(providerScheduleRematchedQueue)
                .to(scheduleExchange)
                .with(PROVIDER_RE_MATCHED_ROUTING_KEY);
    }

    // 페이로드를 JSON으로 주고받기 위한 변환기
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
