package com.todak_todag.schedule_service.global.config;

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

    // 발행용 공통 Exchange
    public static final String SCHEDULE_EXCHANGE = "schedule.exchange";

    // 수신용 Exchange — Provider-Service가 발행 주체
    public static final String PROVIDER_EXCHANGE = "provider.exchange";

    // CarePlanCompleted (발행)
    public static final String CARE_PLAN_COMPLETED_ROUTING_KEY = "schedule.completed.key";
    public static final String CARE_PLAN_SCHEDULE_COMPLETED_QUEUE = "care-plan.schedule-completed.queue";

    // ProviderReMatched (발행)
    public static final String PROVIDER_RE_MATCHED_ROUTING_KEY = "schedule.rematched.key";
    public static final String PROVIDER_SCHEDULE_REMATCHED_QUEUE = "provider.schedule-rematched.queue";

    // ProviderMatched (수신)
    public static final String PROVIDER_MATCHED_ROUTING_KEY = "provider.matched.key";
    public static final String SCHEDULE_PROVIDER_MATCHED_QUEUE = "schedule.provider-matched.queue";

    // ProviderMatchFailed (수신)
    public static final String PROVIDER_MATCH_FAILED_ROUTING_KEY = "provider.match-failed.key";
    public static final String SCHEDULE_PROVIDER_MATCH_FAILED_QUEUE = "schedule.provider-match-failed.queue";

    // 재시도를 소진한 수신 메시지를 보존하는 Dead Letter 경로
    // 원인 해결 후 DLQ 메시지를 원래 큐로 옮겨 다시 처리 (매칭 이력으로 멱등 판별되어 중복 적재되지 않음)
    public static final String SCHEDULE_DLX_EXCHANGE = "schedule.dlx.exchange";
    public static final String SCHEDULE_PROVIDER_MATCHED_DLQ = "schedule.provider-matched.dlq.queue";
    public static final String SCHEDULE_PROVIDER_MATCHED_DLQ_KEY = "schedule.provider-matched.dlq.key";
    public static final String SCHEDULE_PROVIDER_MATCH_FAILED_DLQ = "schedule.provider-match-failed.dlq.queue";
    public static final String SCHEDULE_PROVIDER_MATCH_FAILED_DLQ_KEY = "schedule.provider-match-failed.dlq.key";

    // 아래 두 DLX는 각각 Provider-Service·Care-Plan-Service가 소유
    public static final String PROVIDER_DLX_EXCHANGE = "provider.dlx.exchange";
    public static final String SCHEDULE_REMATCHED_DLQ = "provider.schedule-rematched.dlq.queue";
    public static final String SCHEDULE_REMATCHED_DLQ_KEY = "provider.schedule-rematched.dlq.key";

    public static final String CARE_PLAN_DLX_EXCHANGE = "care-plan.dlx.exchange";
    public static final String CARE_PLAN_SCHEDULE_COMPLETED_DLQ = "care-plan.schedule-completed.dlq.queue";
    public static final String CARE_PLAN_SCHEDULE_COMPLETED_DLQ_ROUTING_KEY = "care-plan.schedule-completed.dlq.key";

    @Bean
    public DirectExchange scheduleExchange() {
        return new DirectExchange(SCHEDULE_EXCHANGE);
    }

    @Bean
    public DirectExchange providerExchange() {
        return new DirectExchange(PROVIDER_EXCHANGE);
    }

    @Bean
    public DirectExchange scheduleDlxExchange() {
        return new DirectExchange(SCHEDULE_DLX_EXCHANGE);
    }

    @Bean
    public DirectExchange providerDlxExchange() {
        return new DirectExchange(PROVIDER_DLX_EXCHANGE);
    }

    @Bean
    public DirectExchange carePlanDlxExchange() {
        return new DirectExchange(CARE_PLAN_DLX_EXCHANGE);
    }

    @Bean
    public Queue carePlanScheduleCompletedQueue() {
        return QueueBuilder
                .durable(CARE_PLAN_SCHEDULE_COMPLETED_QUEUE)
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
                .with(CARE_PLAN_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Queue providerScheduleRematchedQueue() {
        return QueueBuilder
                .durable(PROVIDER_SCHEDULE_REMATCHED_QUEUE)
                .deadLetterExchange(PROVIDER_DLX_EXCHANGE)
                .deadLetterRoutingKey(SCHEDULE_REMATCHED_DLQ_KEY)
                .build();
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

    // ProviderMatched를 수신할 큐
    // 재시도 3회는 리스너 컨테이너 설정으로 처리하고, 소진하면 x-dead-letter-exchange로 DLQ에 옮겨짐
    @Bean
    public Queue scheduleProviderMatchedQueue() {
        return QueueBuilder
                .durable(SCHEDULE_PROVIDER_MATCHED_QUEUE)
                .deadLetterExchange(SCHEDULE_DLX_EXCHANGE)
                .deadLetterRoutingKey(SCHEDULE_PROVIDER_MATCHED_DLQ_KEY)
                .build();
    }

    @Bean
    public Binding scheduleProviderMatchedBinding(
            DirectExchange providerExchange,
            Queue scheduleProviderMatchedQueue
    ) {
        return BindingBuilder.bind(scheduleProviderMatchedQueue)
                .to(providerExchange)
                .with(PROVIDER_MATCHED_ROUTING_KEY);
    }

    // ProviderMatchFailed를 수신할 큐
    @Bean
    public Queue scheduleProviderMatchFailedQueue() {
        return QueueBuilder
                .durable(SCHEDULE_PROVIDER_MATCH_FAILED_QUEUE)
                .deadLetterExchange(SCHEDULE_DLX_EXCHANGE)
                .deadLetterRoutingKey(SCHEDULE_PROVIDER_MATCH_FAILED_DLQ_KEY)
                .build();
    }

    @Bean
    public Binding scheduleProviderMatchFailedBinding(
            DirectExchange providerExchange,
            Queue scheduleProviderMatchFailedQueue
    ) {
        return BindingBuilder.bind(scheduleProviderMatchFailedQueue)
                .to(providerExchange)
                .with(PROVIDER_MATCH_FAILED_ROUTING_KEY);
    }

    @Bean
    public Queue scheduleProviderMatchedDlq() {
        return new Queue(SCHEDULE_PROVIDER_MATCHED_DLQ, true);
    }

    @Bean
    public Binding scheduleProviderMatchedDlqBinding(
            DirectExchange scheduleDlxExchange,
            Queue scheduleProviderMatchedDlq
    ) {
        return BindingBuilder.bind(scheduleProviderMatchedDlq)
                .to(scheduleDlxExchange)
                .with(SCHEDULE_PROVIDER_MATCHED_DLQ_KEY);
    }

    @Bean
    public Queue scheduleProviderMatchFailedDlq() {
        return new Queue(SCHEDULE_PROVIDER_MATCH_FAILED_DLQ, true);
    }

    @Bean
    public Binding scheduleProviderMatchFailedDlqBinding(
            DirectExchange scheduleDlxExchange,
            Queue scheduleProviderMatchFailedDlq
    ) {
        return BindingBuilder.bind(scheduleProviderMatchFailedDlq)
                .to(scheduleDlxExchange)
                .with(SCHEDULE_PROVIDER_MATCH_FAILED_DLQ_KEY);
    }

    @Bean
    public Queue scheduleRematchedDlq() {
        return QueueBuilder
                .durable(SCHEDULE_REMATCHED_DLQ)
                .build();
    }

    @Bean
    public Binding scheduleRematchedDlqBinding(
            DirectExchange providerDlxExchange,
            Queue scheduleRematchedDlq
    ) {
        return BindingBuilder.bind(scheduleRematchedDlq)
                .to(providerDlxExchange)
                .with(SCHEDULE_REMATCHED_DLQ_KEY);
    }

    @Bean
    public Queue carePlanScheduleCompletedDlq() {
        return QueueBuilder
                .durable(CARE_PLAN_SCHEDULE_COMPLETED_DLQ)
                .build();
    }

    @Bean
    public Binding carePlanScheduleCompletedDlqBinding(
            DirectExchange carePlanDlxExchange,
            Queue carePlanScheduleCompletedDlq
    ) {
        return BindingBuilder
                .bind(carePlanScheduleCompletedDlq)
                .to(carePlanDlxExchange)
                .with(CARE_PLAN_SCHEDULE_COMPLETED_DLQ_ROUTING_KEY);
    }

    // 페이로드를 JSON으로 주고받기 위한 변환기
    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }
}
