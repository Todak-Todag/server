package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPublishPort;
import com.todak_todag.provider_service.provider.application.service.command.OutboxEventCommandService;
import com.todak_todag.provider_service.provider.application.service.query.OutboxEventQueryService;
import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("아웃박스 릴레이")
class OutboxRelayFacadeTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    // 나노초까지 왕복되는지에 기대지 않도록 초 단위로 고정한다
    private static final Instant FIXED_TIME = Instant.parse("2026-09-07T09:00:00Z");

    private final UUID carePlanId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID serviceOfferingId = UUID.randomUUID();
    private final UUID outboxEventId = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OutboxEventQueryService outboxEventQueryService;

    @Mock
    private OutboxEventCommandService outboxEventCommandService;

    @Mock
    private MatchingEventPublishPort matchingEventPublishPort;

    private OutboxRelayFacade outboxRelayFacade;

    @BeforeEach
    void setUp() {
        outboxRelayFacade = new OutboxRelayFacade(
                outboxEventQueryService, outboxEventCommandService,
                matchingEventPublishPort, objectMapper
        );
    }

    @Test
    @DisplayName("적재된 매칭 결과를 발행하고 발행 시각을 남긴다")
    void relay_publishesMatched() {
        UUID servicePreferenceId = UUID.randomUUID();
        ProviderMatchedEvent event = matchedEvent(servicePreferenceId);

        given(outboxEventQueryService.findPending(anyInt()))
                .willReturn(List.of(pendingEvent(
                        OutboxEventType.PROVIDER_MATCHED, servicePreferenceId, event
                )));

        outboxRelayFacade.relay();

        verify(matchingEventPublishPort).publishMatched(event);
        verify(outboxEventCommandService).markPublished(outboxEventId);
        verify(outboxEventCommandService, never()).recordFailure(any(), anyString());
    }

    @Test
    @DisplayName("적재된 매칭 실패도 발행한다")
    void relay_publishesMatchFailed() {
        UUID servicePreferenceId = UUID.randomUUID();
        ProviderMatchFailedEvent event = matchFailedEvent(servicePreferenceId);

        given(outboxEventQueryService.findPending(anyInt()))
                .willReturn(List.of(pendingEvent(
                        OutboxEventType.PROVIDER_MATCH_FAILED, servicePreferenceId, event
                )));

        outboxRelayFacade.relay();

        verify(matchingEventPublishPort).publishMatchFailed(event);
        verify(outboxEventCommandService).markPublished(outboxEventId);
    }

    @Test
    @DisplayName("발행에 실패하면 발행 시각을 남기지 않아 다음 폴링에서 다시 시도된다")
    void relay_publishFailure_keepsPending() {
        UUID servicePreferenceId = UUID.randomUUID();

        given(outboxEventQueryService.findPending(anyInt()))
                .willReturn(List.of(pendingEvent(
                        OutboxEventType.PROVIDER_MATCHED, servicePreferenceId,
                        matchedEvent(servicePreferenceId)
                )));
        willThrow(new AmqpException("broker unavailable"))
                .given(matchingEventPublishPort).publishMatched(any());

        assertThatCode(() -> outboxRelayFacade.relay()).doesNotThrowAnyException();

        verify(outboxEventCommandService, never()).markPublished(any());
        verify(outboxEventCommandService).recordFailure(eq(outboxEventId), anyString());
    }

    @Test
    @DisplayName("한 건이 실패해도 나머지는 계속 처리한다")
    void relay_continuesAfterFailure() {
        UUID failingPreferenceId = UUID.randomUUID();
        UUID succeedingPreferenceId = UUID.randomUUID();

        ProviderOutboxEvent failing = pendingEvent(
                OutboxEventType.PROVIDER_MATCHED, failingPreferenceId, matchedEvent(failingPreferenceId)
        );

        ProviderOutboxEvent succeeding = pendingEvent(
                OutboxEventType.PROVIDER_MATCHED, succeedingPreferenceId, matchedEvent(succeedingPreferenceId)
        );

        UUID succeedingId = UUID.randomUUID();
        ReflectionTestUtils.setField(succeeding, "id", succeedingId);

        given(outboxEventQueryService.findPending(anyInt()))
                .willReturn(List.of(failing, succeeding));
        willThrow(new AmqpException("broker unavailable"))
                .given(matchingEventPublishPort).publishMatched(matchedEvent(failingPreferenceId));

        outboxRelayFacade.relay();

        verify(outboxEventCommandService).recordFailure(eq(outboxEventId), anyString());
        verify(outboxEventCommandService).markPublished(succeedingId);
    }

    private ProviderOutboxEvent pendingEvent(OutboxEventType eventType, UUID aggregateId, Object payload) {
        ProviderOutboxEvent outboxEvent = ProviderOutboxEvent.of(
                eventType, aggregateId, objectMapper.writeValueAsString(payload)
        );

        // 식별자는 영속화 시점에 채워지므로 테스트에서 직접 주입한다
        ReflectionTestUtils.setField(outboxEvent, "id", outboxEventId);

        return outboxEvent;
    }

    private ProviderMatchedEvent matchedEvent(UUID servicePreferenceId) {
        return new ProviderMatchedEvent(
                carePlanId, regionId, servicePreferenceId, provideServiceId,
                serviceOfferingId, MONDAY, MONDAY.atTime(9, 0), FIXED_TIME
        );
    }

    private ProviderMatchFailedEvent matchFailedEvent(UUID servicePreferenceId) {
        return new ProviderMatchFailedEvent(
                carePlanId, regionId, servicePreferenceId, provideServiceId,
                MONDAY, TimeSlot.MORNING,
                ProviderMatchFailedEvent.NO_AVAILABLE_PROVIDER, FIXED_TIME
        );
    }
}
