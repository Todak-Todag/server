package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletedEvent;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletedEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEvent;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ScheduleOutboxEventResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ScheduleOutboxQueryService;
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

    @Mock
    private CarePlanCompletedEventPayloadSerializer carePlanCompletedEventPayloadSerializer;

    @Mock
    private CarePlanCompletedEventPort carePlanCompletedEventPort;

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
        ProviderReMatchEvent event =
                providerReMatchEvent();

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
        ProviderReMatchEvent succeedingEvent =
                providerReMatchEvent();

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
        ProviderReMatchEvent event =
                providerReMatchEvent();

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
        verify(carePlanCompletedEventPort, never()).publish(any());
        verify(scheduleOutboxCommandService).recordFailure(eq(outboxEventId), anyString());
    }

    @Test
    void CarePlanCompleted_타입은_CarePlanCompletedEventPort로_발행하고_SENT로_표시한다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        UUID carePlanId = UUID.randomUUID();
        UUID serviceResultId = UUID.randomUUID();
        String payload = "{\"carePlanId\":\"" + carePlanId + "\","
                + "\"serviceResultId\":\"" + serviceResultId + "\",\"status\":\"COMPLETED\"}";

        ScheduleOutboxEventResult pending = new ScheduleOutboxEventResult(
                outboxEventId, CarePlanCompletedEventPort.EVENT_TYPE, carePlanId, payload
        );
        CarePlanCompletedEvent event =
                new CarePlanCompletedEvent(carePlanId, serviceResultId, ScheduleStatus.COMPLETED);

        given(scheduleOutboxQueryService.findPending(anyInt())).willReturn(List.of(pending));
        given(carePlanCompletedEventPayloadSerializer.deserialize(payload)).willReturn(event);

        // when
        scheduleOutboxRelayFacade.relay();

        // then
        verify(carePlanCompletedEventPort).publish(event);
        verify(providerReMatchEventPort, never()).publish(any());
        verify(scheduleOutboxCommandService).markSent(outboxEventId);
        verify(scheduleOutboxCommandService, never()).recordFailure(any(), anyString());
    }

    @Test
    void CarePlanCompleted_발행이_실패하면_recordFailure로_기록한다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        ScheduleOutboxEventResult pending = new ScheduleOutboxEventResult(
                outboxEventId, CarePlanCompletedEventPort.EVENT_TYPE, UUID.randomUUID(), "{}"
        );
        CarePlanCompletedEvent event =
                new CarePlanCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), ScheduleStatus.CANCELED);

        given(scheduleOutboxQueryService.findPending(anyInt())).willReturn(List.of(pending));
        given(carePlanCompletedEventPayloadSerializer.deserialize("{}")).willReturn(event);
        willThrow(new RuntimeException("broker unavailable")).given(carePlanCompletedEventPort).publish(event);

        // when
        scheduleOutboxRelayFacade.relay();

        // then
        verify(scheduleOutboxCommandService).recordFailure(eq(outboxEventId), anyString());
        verify(scheduleOutboxCommandService, never()).markSent(outboxEventId);
    }

    private ProviderReMatchEvent providerReMatchEvent() {
        return ProviderReMatchEvent.forScheduleChange(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1)
        );
    }
}
