package com.todak_todag.provider_service.provider.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("아웃박스 이벤트")
class ProviderOutboxEventTest {

    private ProviderOutboxEvent failedTimes(int count) {
        ProviderOutboxEvent event = ProviderOutboxEvent.of(OutboxEventType.PROVIDER_MATCHED, UUID.randomUUID(), "{}");
        for (int i = 0; i < count; i++) {
            event.recordFailure("발행 실패");
        }
        return event;
    }

    @Test
    @DisplayName("재시도 한도 직전까지는 소진되지 않는다")
    void notExhausted_belowMax() {
        ProviderOutboxEvent event = failedTimes(ProviderOutboxEvent.MAX_RETRY_COUNT - 1);

        assertThat(event.isRetryExhausted()).isFalse();
    }

    @Test
    @DisplayName("재시도 한도에 닿으면 소진된다")
    void exhausted_atMax() {
        ProviderOutboxEvent event = failedTimes(ProviderOutboxEvent.MAX_RETRY_COUNT);

        assertThat(event.isRetryExhausted()).isTrue();
        assertThat(event.getLastErrorMessage()).isEqualTo("발행 실패");
    }
}
