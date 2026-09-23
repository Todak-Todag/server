package com.todak_todag.social_worker_service.matching.infrastructure.messaging;

import com.todak_todag.social_worker_service.global.config.RabbitMqConfig;
import com.todak_todag.social_worker_service.matching.application.event.CarePlanCompletedEvent;
import com.todak_todag.social_worker_service.matching.application.service.command.CarePlanCompletedEventService;
import com.todak_todag.social_worker_service.matching.application.support.idempotency.EventIdempotencyStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanCompletedEventConsumer {

    private final CarePlanCompletedEventService carePlanCompletedEventService;
    private final EventIdempotencyStore eventIdempotencyStore;

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

        if (!eventIdempotencyStore.tryAcquire(
                event.eventId()
        )) {

            log.info(
                    "[SocialWorkerMatching] 중복 CarePlanCompleted 이벤트 무시 "
                            + "eventId={}, carePlanId={}, patientId={}",
                    event.eventId(),
                    event.carePlanId(),
                    event.patientId()
            );

            return;
        }

        try {

            carePlanCompletedEventService.handle(
                    event
            );

        } catch (RuntimeException e) {

            eventIdempotencyStore.release(
                    event.eventId()
            );

            throw e;
        }
    }
}