package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanEventConsumer {
    private final CarePlanCommandService carePlanCommandService;

    @RabbitListener(
            queues = RabbitMqConfig.CARE_PLAN_COMPLETED_QUEUE
    )
    public void consumeCarePlanCompleted(
            CarePlanCompletedEvent carePlanCompletedEvent
    ) {
        carePlanCommandService.completeCarePlan(
                carePlanCompletedEvent.carePlanId()
        );
    }
}
