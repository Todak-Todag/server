package com.spring.careplanservice.global.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String SCHEDULE_EXCHANGE = "schedule.exchange";
    public static final String SCHEDULE_COMPLETED_ROUTING_KEY = "schedule.completed.key";
    public static final String CARE_PLAN_SCHEDULE_COMPLETED_QUEUE =
            "care-plan.schedule-completed.queue";
    public static final String CARE_PLAN_CONFIRMED_EXCHANGE = "care-plan.exchange";
    public static final String CARE_PLAN_CONFIRMED_ROUTING_KEY = "care-plan.confirmed.key";

    public static final String CARE_PLAN_COMPLETED_EXCHANGE = "care-plan.exchange";
    public static final String CARE_PLAN_COMPLETED_ROUTING_KEY = "care-plan.completed.key";

    // CarePlanCompleted Consumer Retry 소진 시 이동하는 Dead Letter 경로
    // care-plan-service가 공유하는 단일 DLX — 향후 다른 Consumer 큐가 늘어나도 이 DLX를 재사용한다
    public static final String CARE_PLAN_DLX_EXCHANGE = "care-plan.dlx.exchange";
    public static final String CARE_PLAN_SCHEDULE_COMPLETED_DLQ = "care-plan.schedule-completed.dlq.queue";
    public static final String CARE_PLAN_SCHEDULE_COMPLETED_DLQ_ROUTING_KEY = "care-plan.schedule-completed.dlq.key";

    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();

        typeMapper.setTypePrecedence(
                JacksonJavaTypeMapper.TypePrecedence.INFERRED
        );

        typeMapper.setTrustedPackages("*");

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public DirectExchange carePlanConfirmedExchange() {
        return new DirectExchange(CARE_PLAN_CONFIRMED_EXCHANGE);
    }

    @Bean
    public DirectExchange scheduleExchange() {
        return new DirectExchange(SCHEDULE_EXCHANGE);
    }

    // Retry(spring.rabbitmq.listener.simple.retry)를 모두 소진한 메시지는
    // x-dead-letter-exchange / x-dead-letter-routing-key를 통해 DLQ로 이동한다
    @Bean
    public Queue carePlanScheduleCompletedQueue() {
        return QueueBuilder.durable(CARE_PLAN_SCHEDULE_COMPLETED_QUEUE)
                .deadLetterExchange(CARE_PLAN_DLX_EXCHANGE)
                .deadLetterRoutingKey(CARE_PLAN_SCHEDULE_COMPLETED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding carePlanScheduleCompletedBinding(
            DirectExchange scheduleExchange,
            Queue carePlanScheduleCompletedQueue
    ) {
        return BindingBuilder.bind(carePlanScheduleCompletedQueue)
                .to(scheduleExchange)
                .with(SCHEDULE_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange carePlanDlxExchange() {
        return new DirectExchange(CARE_PLAN_DLX_EXCHANGE);
    }

    @Bean
    public Queue carePlanScheduleCompletedDlq() {
        return new Queue(CARE_PLAN_SCHEDULE_COMPLETED_DLQ, true);
    }

    @Bean
    public Binding carePlanScheduleCompletedDlqBinding(
            DirectExchange carePlanDlxExchange,
            Queue carePlanScheduleCompletedDlq
    ) {
        return BindingBuilder.bind(carePlanScheduleCompletedDlq)
                .to(carePlanDlxExchange)
                .with(CARE_PLAN_SCHEDULE_COMPLETED_DLQ_ROUTING_KEY);
    }
}
