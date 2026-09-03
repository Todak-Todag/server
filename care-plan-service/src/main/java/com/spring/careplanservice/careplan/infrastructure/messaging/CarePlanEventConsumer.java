package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanEventConsumer {
    private final CarePlanCommandService carePlanCommandService;

    @RabbitListener(
            queues = "${rabbitmq.care-plan-completed.queue}"
    )
    public void consumeCarePlanCompleted(
            CarePlanCompletedEvent carePlanCompletedEvent
    ) {
        carePlanCommandService.completeCarePlan(
                carePlanCompletedEvent.carePlanId()
        );
    }
}
