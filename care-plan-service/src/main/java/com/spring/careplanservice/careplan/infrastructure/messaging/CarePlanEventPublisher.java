package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    // TODO: Care Plan 수정 API 구현 시 UNDER_REVIEW -> CONFIRMED 전환 후 CarePlanConfirmed 이벤트 발행 연결
    public void publishCarePlanConfirmed(
            CarePlanConfirmedEvent carePlanConfirmedEvent
    ) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CARE_PLAN_CONFIRMED_EXCHANGE,
                RabbitMqConfig.CARE_PLAN_CONFIRMED_ROUTING_KEY,
                carePlanConfirmedEvent
        );
    }
}
