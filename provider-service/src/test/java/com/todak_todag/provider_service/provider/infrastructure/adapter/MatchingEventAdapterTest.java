package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;
import com.todak_todag.provider_service.provider.application.service.command.OutboxEventCommandService;
import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("매칭 이벤트 아웃박스 적재")
class MatchingEventAdapterTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    // 나노초까지 왕복되는지에 기대지 않도록 초 단위로 고정한다
    private static final Instant FIXED_TIME = Instant.parse("2026-09-07T09:00:00Z");

    private final UUID carePlanId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID servicePreferenceId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID serviceOfferingId = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OutboxEventCommandService outboxEventCommandService;

    private MatchingEventAdapter matchingEventAdapter;

    @BeforeEach
    void setUp() {
        matchingEventAdapter = new MatchingEventAdapter(outboxEventCommandService, objectMapper);
    }

    @Test
    @DisplayName("매칭 결과를 브로커로 바로 보내지 않고 아웃박스에 적재한다")
    void publishMatched_appendsToOutbox() {
        ProviderMatchedEvent event = new ProviderMatchedEvent(
                carePlanId, regionId, servicePreferenceId, provideServiceId,
                serviceOfferingId, MONDAY, MONDAY.atTime(9, 0), FIXED_TIME
        );

        matchingEventAdapter.publishMatched(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxEventCommandService).append(
                eq(OutboxEventType.PROVIDER_MATCHED),
                eq(servicePreferenceId),
                payload.capture()
        );

        // 릴레이가 같은 ObjectMapper로 되돌리므로 왕복까지 확인한다
        assertThat(objectMapper.readValue(payload.getValue(), ProviderMatchedEvent.class))
                .isEqualTo(event);
    }

    @Test
    @DisplayName("매칭 실패도 아웃박스에 적재한다")
    void publishMatchFailed_appendsToOutbox() {
        ProviderMatchFailedEvent event = new ProviderMatchFailedEvent(
                carePlanId, regionId, servicePreferenceId, provideServiceId,
                MONDAY, TimeSlot.MORNING,
                ProviderMatchFailedEvent.NO_AVAILABLE_PROVIDER, FIXED_TIME
        );

        matchingEventAdapter.publishMatchFailed(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxEventCommandService).append(
                eq(OutboxEventType.PROVIDER_MATCH_FAILED),
                eq(servicePreferenceId),
                payload.capture()
        );

        assertThat(objectMapper.readValue(payload.getValue(), ProviderMatchFailedEvent.class))
                .isEqualTo(event);
    }
}
