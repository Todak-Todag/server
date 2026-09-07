package com.todak_todag.schedule_service.global.config;

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

    @Bean
    public DirectExchange scheduleExchange() {
        return new DirectExchange(SCHEDULE_EXCHANGE);
    }

    @Bean
    public DirectExchange providerExchange() {
        return new DirectExchange(PROVIDER_EXCHANGE);
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

    // ProviderMatched를 수신할 큐
    // 재시도 3회는 리스너 컨테이너 설정(spring.rabbitmq.listener.simple.retry)으로 처리
    @Bean
    public Queue scheduleProviderMatchedQueue() {
        return new Queue(SCHEDULE_PROVIDER_MATCHED_QUEUE, true);
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
        return new Queue(SCHEDULE_PROVIDER_MATCH_FAILED_QUEUE, true);
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
