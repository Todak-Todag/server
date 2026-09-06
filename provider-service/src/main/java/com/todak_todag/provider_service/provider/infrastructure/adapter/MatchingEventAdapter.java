package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.global.config.RabbitMqConfig;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingEventAdapter implements MatchingEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishMatched(ProviderMatchedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                RabbitMqConfig.PROVIDER_MATCHED_KEY,
                event
        );
    }

    @Override
    public void publishMatchFailed(ProviderMatchFailedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                RabbitMqConfig.PROVIDER_MATCH_FAILED_KEY,
                event
        );
    }
}