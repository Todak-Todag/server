package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletedEvent;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// CarePlanCompleted 이벤트를 실제 RabbitMQ로 발행하는 Adapter
//
// 호출 주체는 커맨드 트랜잭션이 아니라 아웃박스 릴레이(ScheduleOutboxRelayFacade)
// 즉 이 시점은 이미 DB 커밋이 끝난 뒤이므로, 발행 실패는 아웃박스 레코드의 재시도로 처리
@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanCompletedEventRabbitAdapter implements CarePlanCompletedEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(CarePlanCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SCHEDULE_EXCHANGE,
                RabbitMqConfig.CARE_PLAN_COMPLETED_ROUTING_KEY,
                event
        );

        log.info("[Schedule] CarePlanCompleted 이벤트 발행 serviceResultId={}", event.serviceResultId());
    }
}
