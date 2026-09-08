package com.spring.careplanservice.careplan.infrastructure.adapter;


import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEvent;
import com.spring.careplanservice.careplan.application.port.CarePlanCompletedEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import com.spring.careplanservice.global.config.RabbitMqConfig;

// CarePlan 완료 이벤트를 실제 RabbitMQ로 발행하는 Adapter
//
// 호출 주체는 CarePlan 상태 변경 트랜잭션이 아니라 Outbox Relay.
// 따라서 이 시점에는 CarePlan COMPLETED 변경과 Outbox INSERT가 이미 커밋된 상태이며,
// 발행 실패는 Outbox의 retryCount를 통해 재시도한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanCompletedEventRabbitAdapter implements CarePlanCompletedEventPort {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(CarePlanCompletionEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CARE_PLAN_COMPLETED_EXCHANGE,
                RabbitMqConfig.CARE_PLAN_COMPLETED_ROUTING_KEY,
                event
        );

        log.info(
                "[CarePlan] CarePlanCompleted 이벤트 발행 carePlanId={}",
                event.carePlanId()
        );
    }
}
