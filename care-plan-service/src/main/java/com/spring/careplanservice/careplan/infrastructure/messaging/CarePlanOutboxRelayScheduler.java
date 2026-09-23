package com.spring.careplanservice.careplan.infrastructure.messaging;

import com.spring.careplanservice.careplan.application.facade.CarePlanOutboxRelayFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Outbox에 쌓인 CarePlan 완료 이벤트를 주기적으로 조회하여 RabbitMQ 발행을 시도

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "care-plan.outbox.relay.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CarePlanOutboxRelayScheduler {
    private final CarePlanOutboxRelayFacade carePlanOutboxRelayFacade;

    /**
     * 이전 실행이 끝난 뒤 5초 후 다시 실행한다.
     *
     * fixedDelay 방식이므로 relay 처리 시간이 길어져도
     * 이전 작업이 끝나기 전에 다음 작업이 겹쳐 실행되지 않는다.
     */
    @Scheduled(
            fixedDelayString = "${care-plan.outbox.relay.fixed-delay-ms:5000}"
    )
    public void relay() {
        carePlanOutboxRelayFacade.relay();
    }
}
