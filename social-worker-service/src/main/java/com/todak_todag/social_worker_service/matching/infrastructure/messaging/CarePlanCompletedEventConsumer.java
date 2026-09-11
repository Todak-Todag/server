package com.todak_todag.social_worker_service.matching.infrastructure.messaging;

import com.todak_todag.social_worker_service.global.config.RabbitMqConfig;
import com.todak_todag.social_worker_service.matching.application.event.CarePlanCompletedEvent;
import com.todak_todag.social_worker_service.matching.application.service.command.CarePlanCompletedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanCompletedEventConsumer {

    private final CarePlanCompletedEventService carePlanCompletedEventService;

    @RabbitListener(
            queues = RabbitMqConfig.SOCIAL_WORKER_CARE_PLAN_COMPLETED_QUEUE
    )
    public void consume(
            CarePlanCompletedEvent event
    ) {

        log.info(
                "[SocialWorkerMatching] CarePlanCompleted 이벤트 수신 "
                        + "eventId={}, carePlanId={}, patientId={}, completedAt={}",
                event.eventId(),
                event.carePlanId(),
                event.patientId(),
                event.completedAt()
        );

        carePlanCompletedEventService.handle(
                event
        );
    }
}