package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchFailedEvent;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceMatchingCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// ProviderMatchFailed 이벤트 수신 진입점
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderMatchFailedEventListener {

    private final ServiceMatchingCommandService serviceMatchingCommandService;

    @RabbitListener(queues = RabbitMqConfig.SCHEDULE_PROVIDER_MATCH_FAILED_QUEUE)
    public void handle(ProviderMatchFailedEvent event) {
        log.info(
                "[Schedule] ProviderMatchFailed 이벤트 수신 carePlanId={} servicePreferenceId={} date={}",
                event.carePlanId(), event.servicePreferenceId(), event.date()
        );

        try {
            serviceMatchingCommandService.applyMatchFailed(event);
        } catch (BusinessException e) {
            log.error(
                    "[Schedule] ProviderMatchFailed 이벤트 처리 실패 — 재시도해도 동일하므로 건너뜁니다 "
                            + "errorCode={} carePlanId={} servicePreferenceId={} date={}",
                    e.getErrorCode().getCode(), event.carePlanId(), event.servicePreferenceId(), event.date(), e
            );
        }
    }
}
