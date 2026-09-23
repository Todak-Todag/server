package com.todak_todag.social_worker_service.global.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String CARE_PLAN_EXCHANGE =
            "care-plan.exchange";

    public static final String CARE_PLAN_COMPLETED_ROUTING_KEY =
            "care-plan.completed.key";

    public static final String SOCIAL_WORKER_CARE_PLAN_COMPLETED_QUEUE =
            "social-worker.care-plan-completed.queue";

    @Bean
    public DirectExchange carePlanExchange() {
        return new DirectExchange(
                CARE_PLAN_EXCHANGE
        );
    }

    @Bean
    public Queue socialWorkerCarePlanCompletedQueue() {
        return new Queue(
                SOCIAL_WORKER_CARE_PLAN_COMPLETED_QUEUE,
                true
        );
    }

    @Bean
    public Binding socialWorkerCarePlanCompletedBinding(
            DirectExchange carePlanExchange,
            Queue socialWorkerCarePlanCompletedQueue
    ) {
        return BindingBuilder
                .bind(
                        socialWorkerCarePlanCompletedQueue
                )
                .to(
                        carePlanExchange
                )
                .with(
                        CARE_PLAN_COMPLETED_ROUTING_KEY
                );
    }

    @Bean
    public MessageConverter messageConverter() {

        JacksonJsonMessageConverter converter =
                new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper typeMapper =
                new DefaultJacksonJavaTypeMapper();

        typeMapper.setTypePrecedence(
                JacksonJavaTypeMapper.TypePrecedence.INFERRED
        );

        typeMapper.setTrustedPackages("*");

        converter.setJavaTypeMapper(
                typeMapper
        );

        return converter;
    }
}