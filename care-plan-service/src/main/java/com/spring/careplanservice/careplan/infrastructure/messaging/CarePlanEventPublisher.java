package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.care-plan-confirmed.exchange}")
    private String exchange;

    @Value("${rabbitmq.care-plan-confirmed.routing-key}")
    private String routingKey;

    public void publishCarePlanConfirmed(
            CarePlanConfirmedEvent carePlanConfirmedEvent
    ) {
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                carePlanConfirmedEvent
        );
    }
}
