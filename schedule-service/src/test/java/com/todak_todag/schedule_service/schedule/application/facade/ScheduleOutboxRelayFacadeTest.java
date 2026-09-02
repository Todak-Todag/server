package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ScheduleOutboxEventResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ScheduleOutboxQueryService;
import com.todak_todag.schedule_service.schedule.application.support.ProviderReMatchEventPayloadSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 실제 발행 대상(ProviderReMatchEventPort)은 아직 NoOp 스텁이라, 여기서는 실패 격리와
// 이벤트 상태 갱신 호출이 올바른지만 검증한다(각 이벤트는 서로 독립된 실패 단위여야 한다).
@ExtendWith(MockitoExtension.class)
class ScheduleOutboxRelayFacadeTest {

    @Mock
    private ScheduleOutboxQueryService scheduleOutboxQueryService;

    @Mock
    private ScheduleOutboxCommandService scheduleOutboxCommandService;

    @Mock
    private ProviderReMatchEventPayloadSerializer providerReMatchEventPayloadSerializer;

    @Mock
    private ProviderReMatchEventPort providerReMatchEventPort;

    @InjectMocks
    private ScheduleOutboxRelayFacade scheduleOutboxRelayFacade;

    @Test
    void 대기중인_이벤트가_없으면_아무것도_하지_않는다() {
        // given
        given(scheduleOutboxQueryService.findPending(anyInt())).willReturn(List.of());

        // when
        scheduleOutboxRelayFacade.relay();

        // then
        verify(providerReMatchEventPort, never()).publish(any());
        verify(scheduleOutboxCommandService, never()).markSent(any());
        verify(scheduleOutboxCommandService, never()).recordFailure(any(), anyString());
    }

    @Test
    void 발행에_성공하면_해당_이벤트를_SENT로_표시한다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        UUID serviceScheduleId = UUID.randomUUID();
        ScheduleOutboxEventResult pending = new ScheduleOutboxEventResult(
                outboxEventId, ProviderReMatchEventPort.EVENT_TYPE, serviceScheduleId, "{}"
        );
        ProviderReMatchEventPort.ProviderReMatchEvent event =
                new ProviderReMatchEventPort.ProviderReMatchEvent(serviceScheduleId, UUID.randomUUID(), LocalDate.now().plusDays(1));

        given(scheduleOutboxQueryService.findPending(anyInt())).willReturn(List.of(pending));
        given(providerReMatchEventPayloadSerializer.deserialize("{}")).willReturn(event);

        // when
        scheduleOutboxRelayFacade.relay();

        // then
        verify(providerReMatchEventPort).publish(event);
        verify(scheduleOutboxCommandService).markSent(outboxEventId);
        verify(scheduleOutboxCommandService, never()).recordFailure(any(), anyString());
    }

    @Test
    void 발행이_실패한_이벤트는_recordFailure로_기록하고_다른_이벤트_처리는_계속한다() {
        // given
        UUID failingId = UUID.randomUUID();
        UUID succeedingId = UUID.randomUUID();
        ScheduleOutboxEventResult failing = new ScheduleOutboxEventResult(
                failingId, ProviderReMatchEventPort.EVENT_TYPE, UUID.randomUUID(), "{\"broken\":true}"
        );
        ScheduleOutboxEventResult succeeding = new ScheduleOutboxEventResult(
                succeedingId, ProviderReMatchEventPort.EVENT_TYPE, UUID.randomUUID(), "{}"
        );
        ProviderReMatchEventPort.ProviderReMatchEvent succeedingEvent =
                new ProviderReMatchEventPort.ProviderReMatchEvent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1));

        given(scheduleOutboxQueryService.findPending(anyInt())).willReturn(List.of(failing, succeeding));
        willThrow(new IllegalStateException("역직렬화 실패"))
                .given(providerReMatchEventPayloadSerializer).deserialize("{\"broken\":true}");
        willReturn(succeedingEvent)
                .given(providerReMatchEventPayloadSerializer).deserialize("{}");

        // when
        scheduleOutboxRelayFacade.relay();

        // then
        verify(scheduleOutboxCommandService).recordFailure(eq(failingId), anyString());
        verify(scheduleOutboxCommandService, never()).markSent(failingId);

        verify(providerReMatchEventPort).publish(succeedingEvent);
        verify(scheduleOutboxCommandService).markSent(succeedingId);
    }

    @Test
    void publish가_예외를_던지면_recordFailure로_기록한다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        ScheduleOutboxEventResult pending = new ScheduleOutboxEventResult(
                outboxEventId, ProviderReMatchEventPort.EVENT_TYPE, UUID.randomUUID(), "{}"
        );
        ProviderReMatchEventPort.ProviderReMatchEvent event =
                new ProviderReMatchEventPort.ProviderReMatchEvent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1));

        given(scheduleOutboxQueryService.findPending(anyInt())).willReturn(List.of(pending));
        given(providerReMatchEventPayloadSerializer.deserialize("{}")).willReturn(event);
        willThrow(new RuntimeException("broker unavailable")).given(providerReMatchEventPort).publish(event);

        // when
        scheduleOutboxRelayFacade.relay();

        // then
        verify(scheduleOutboxCommandService).recordFailure(eq(outboxEventId), anyString());
        verify(scheduleOutboxCommandService, never()).markSent(outboxEventId);
    }

    @Test
    void 알수_없는_이벤트_타입은_recordFailure로_기록하고_역직렬화를_시도하지_않는다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        ScheduleOutboxEventResult pending = new ScheduleOutboxEventResult(
                outboxEventId, "UnknownEvent", UUID.randomUUID(), "{}"
        );
        given(scheduleOutboxQueryService.findPending(anyInt())).willReturn(List.of(pending));

        // when
        scheduleOutboxRelayFacade.relay();

        // then
        verify(providerReMatchEventPayloadSerializer, never()).deserialize(anyString());
        verify(providerReMatchEventPort, never()).publish(any());
        verify(scheduleOutboxCommandService).recordFailure(eq(outboxEventId), anyString());
    }
}
