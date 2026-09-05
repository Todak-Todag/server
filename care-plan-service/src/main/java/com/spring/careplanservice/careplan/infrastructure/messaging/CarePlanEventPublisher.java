package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import com.spring.careplanservice.careplan.application.port.CarePlanEventPort;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanEventPublisher implements CarePlanEventPort {
    private final RabbitTemplate rabbitTemplate;

    @Override
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
