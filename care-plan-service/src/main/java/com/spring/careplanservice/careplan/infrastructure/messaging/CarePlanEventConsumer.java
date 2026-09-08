package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanEventConsumer {
    private final CarePlanCommandService carePlanCommandService;

    @RabbitListener(
            queues = RabbitMqConfig.CARE_PLAN_SCHEDULE_COMPLETED_QUEUE
    )
    public void consumeCarePlanCompleted(
            CarePlanCompletedEvent carePlanCompletedEvent
    ) {
        log.info(
                "[CarePlan] CarePlanCompleted 이벤트 수신 carePlanId={} serviceResultId={} status={}",
                carePlanCompletedEvent.carePlanId(),
                carePlanCompletedEvent.serviceResultId(),
                carePlanCompletedEvent.status()
        );

        carePlanCommandService.completeCarePlan(
                carePlanCompletedEvent
        );

        log.info(
                "[CarePlan] CarePlanCompleted 이벤트 처리 완료 carePlanId={}",
                carePlanCompletedEvent.carePlanId()
        );
    }
}
