package com.todak_todag.provider_service.global.config;

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

    // 수신 — Care-Plan / Schedule이 소유한 Exchange
    public static final String CARE_PLAN_EXCHANGE = "care-plan.exchange";
    public static final String CARE_PLAN_CONFIRMED_KEY = "care-plan.confirmed.key";
    public static final String CARE_PLAN_CONFIRMED_QUEUE = "provider.care-plan-confirmed.queue";

    public static final String SCHEDULE_EXCHANGE = "schedule.exchange";
    public static final String SCHEDULE_REMATCHED_KEY = "schedule.rematched.key";
    public static final String SCHEDULE_REMATCHED_QUEUE = "provider.schedule-rematched.queue";

    // 발행 — Provider가 소유한 Exchange
    public static final String PROVIDER_EXCHANGE = "provider.exchange";
    public static final String PROVIDER_MATCHED_KEY = "provider.matched.key";
    public static final String PROVIDER_MATCH_FAILED_KEY = "provider.match-failed.key";

    // 재시도를 소진한 수신 메시지를 보존하는 Dead Letter 경로
    // 원인 해결 후 DLQ 메시지를 원래 큐로 옮겨 다시 처리한다 (처리 기록으로 중복 적재되지 않음)
    public static final String PROVIDER_DLX_EXCHANGE = "provider.dlx.exchange";
    public static final String CARE_PLAN_CONFIRMED_DLQ = "provider.care-plan-confirmed.dlq.queue";
    public static final String CARE_PLAN_CONFIRMED_DLQ_KEY = "provider.care-plan-confirmed.dlq.key";
    public static final String SCHEDULE_REMATCHED_DLQ = "provider.schedule-rematched.dlq.queue";
    public static final String SCHEDULE_REMATCHED_DLQ_KEY = "provider.schedule-rematched.dlq.key";

    @Bean
    public DirectExchange carePlanExchange() {
        return new DirectExchange(CARE_PLAN_EXCHANGE);
    }

    @Bean
    public DirectExchange scheduleExchange() {
        return new DirectExchange(SCHEDULE_EXCHANGE);
    }

    @Bean
    public DirectExchange providerExchange() {
        return new DirectExchange(PROVIDER_EXCHANGE);
    }

    // 리스너 재시도를 모두 소진하면 x-dead-letter-exchange로 DLQ에 옮겨진다
    @Bean
    public Queue carePlanConfirmedQueue() {
        return QueueBuilder.durable(CARE_PLAN_CONFIRMED_QUEUE)
                .deadLetterExchange(PROVIDER_DLX_EXCHANGE)
                .deadLetterRoutingKey(CARE_PLAN_CONFIRMED_DLQ_KEY)
                .build();
    }

    @Bean
    public Queue scheduleRematchedQueue() {
        return QueueBuilder.durable(SCHEDULE_REMATCHED_QUEUE)
                .deadLetterExchange(PROVIDER_DLX_EXCHANGE)
                .deadLetterRoutingKey(SCHEDULE_REMATCHED_DLQ_KEY)
                .build();
    }

    @Bean
    public Binding carePlanConfirmedBinding() {
        return BindingBuilder.bind(carePlanConfirmedQueue())
                .to(carePlanExchange())
                .with(CARE_PLAN_CONFIRMED_KEY);
    }

    @Bean
    public Binding scheduleRematchedBinding() {
        return BindingBuilder.bind(scheduleRematchedQueue())
                .to(scheduleExchange())
                .with(SCHEDULE_REMATCHED_KEY);
    }

    @Bean
    public DirectExchange providerDlxExchange() {
        return new DirectExchange(PROVIDER_DLX_EXCHANGE);
    }

    @Bean
    public Queue carePlanConfirmedDlq() {
        return new Queue(CARE_PLAN_CONFIRMED_DLQ, true);
    }

    @Bean
    public Queue scheduleRematchedDlq() {
        return new Queue(SCHEDULE_REMATCHED_DLQ, true);
    }

    @Bean
    public Binding carePlanConfirmedDlqBinding() {
        return BindingBuilder.bind(carePlanConfirmedDlq())
                .to(providerDlxExchange())
                .with(CARE_PLAN_CONFIRMED_DLQ_KEY);
    }

    @Bean
    public Binding scheduleRematchedDlqBinding() {
        return BindingBuilder.bind(scheduleRematchedDlq())
                .to(providerDlxExchange())
                .with(SCHEDULE_REMATCHED_DLQ_KEY);
    }

    // Care-Plan·Schedule이 발행할 때 컨버터가 __TypeId__ 헤더에 자기 쪽 클래스 이름을 박는다
    // 그 클래스는 provider에 존재하지 않아 기본 설정으로는 역직렬화가 ClassNotFoundException으로 실패한다
    // 리스너 파라미터 타입을 우선하도록 바꿔 헤더를 무시하게 한다
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