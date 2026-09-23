package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchedEvent;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceMatchingCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// ProviderMatched 이벤트 수신 진입점
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderMatchedEventListener {

    private final ServiceMatchingCommandService serviceMatchingCommandService;

    @RabbitListener(queues = RabbitMqConfig.SCHEDULE_PROVIDER_MATCHED_QUEUE)
    public void handle(ProviderMatchedEvent event) {
        log.info(
                "[Schedule] ProviderMatched 이벤트 수신 carePlanId={} servicePreferenceId={} serviceOfferingId={} date={}",
                event.carePlanId(), event.servicePreferenceId(), event.serviceOfferingId(), event.date()
        );

        try {
            serviceMatchingCommandService.applyMatched(event);
        } catch (BusinessException e) {
            // 재시도를 소진한 뒤 DLQ에 보존해, 원인(데이터 이상/페이로드 오류)을 고친 후 재투입할 수 있게 함
            log.error(
                    "[Schedule] ProviderMatched 이벤트 처리 실패 — 재시도 소진 시 DLQ로 이동합니다 "
                            + "errorCode={} carePlanId={} servicePreferenceId={} date={}",
                    e.getErrorCode().getCode(), event.carePlanId(), event.servicePreferenceId(), event.date(), e
            );

            throw e;
        }
    }
}
