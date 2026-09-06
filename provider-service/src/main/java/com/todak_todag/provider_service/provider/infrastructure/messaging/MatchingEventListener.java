package com.todak_todag.provider_service.provider.infrastructure.messaging;

import com.todak_todag.provider_service.global.config.RabbitMqConfig;
import com.todak_todag.provider_service.provider.application.event.CarePlanConfirmedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderRematchedEvent;
import com.todak_todag.provider_service.provider.application.facade.MatchingFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingEventListener {

    private final MatchingFacade matchingFacade;

    @RabbitListener(queues = RabbitMqConfig.CARE_PLAN_CONFIRMED_QUEUE)
    public void onCarePlanConfirmed(CarePlanConfirmedEvent event) {
        log.info("[Provider] CarePlanConfirmed 수신 carePlanId={}", event.carePlanId());

        matchingFacade.match(event);
    }

    @RabbitListener(queues = RabbitMqConfig.SCHEDULE_REMATCHED_QUEUE)
    public void onProviderRematched(ProviderRematchedEvent event) {
        log.info("[Provider] ProviderRematched 수신 servicePreferenceId={}", event.servicePreferenceId());

        matchingFacade.rematch(event);
    }
}