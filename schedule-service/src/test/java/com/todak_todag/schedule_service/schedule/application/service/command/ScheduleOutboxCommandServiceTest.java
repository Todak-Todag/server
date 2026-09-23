package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.schedule.domain.entity.OutboxEventStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleOutboxCommandServiceTest {

    @Mock
    private ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;

    @InjectMocks
    private ScheduleOutboxCommandService scheduleOutboxCommandService;

    @Test
    void enqueue하면_PENDING_상태의_아웃박스_이벤트를_저장한다() {
        // given
        UUID aggregateId = UUID.randomUUID();
        String payload = "{\"key\":\"value\"}";
        when(scheduleOutboxEventCommandRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        scheduleOutboxCommandService.enqueue("ProviderReMatched", aggregateId, payload);

        // then
        ArgumentCaptor<ScheduleOutboxEvent> captor = ArgumentCaptor.forClass(ScheduleOutboxEvent.class);
        verify(scheduleOutboxEventCommandRepository).save(captor.capture());
        ScheduleOutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("ProviderReMatched");
        assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
        assertThat(saved.getPayload()).isEqualTo(payload);
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }

    @Test
    void markSent은_이벤트를_찾아_SENT로_변경한다() {
        // given
        ScheduleOutboxEvent event = ScheduleOutboxEvent.create("ProviderReMatched", UUID.randomUUID(), "{}");
        UUID outboxEventId = UUID.randomUUID();
        when(scheduleOutboxEventCommandRepository.findById(outboxEventId)).thenReturn(Optional.of(event));
        when(scheduleOutboxEventCommandRepository.save(event)).thenReturn(event);

        // when
        scheduleOutboxCommandService.markSent(outboxEventId);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(scheduleOutboxEventCommandRepository).save(event);
    }

    @Test
    void markSent은_존재하지_않는_이벤트면_예외를_던진다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        when(scheduleOutboxEventCommandRepository.findById(outboxEventId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleOutboxCommandService.markSent(outboxEventId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordFailure는_재시도_횟수를_증가시키고_PENDING을_유지한다() {
        // given
        ScheduleOutboxEvent event = ScheduleOutboxEvent.create("ProviderReMatched", UUID.randomUUID(), "{}");
        UUID outboxEventId = UUID.randomUUID();
        when(scheduleOutboxEventCommandRepository.findById(outboxEventId)).thenReturn(Optional.of(event));
        when(scheduleOutboxEventCommandRepository.save(event)).thenReturn(event);

        // when
        scheduleOutboxCommandService.recordFailure(outboxEventId, "connection refused");

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastErrorMessage()).isEqualTo("connection refused");
        verify(scheduleOutboxEventCommandRepository).save(event);
    }

    @Test
    void recordFailure는_재시도_상한에_도달하면_FAILED로_전환한다() {
        // given
        ScheduleOutboxEvent event = ScheduleOutboxEvent.create("ProviderReMatched", UUID.randomUUID(), "{}");
        UUID outboxEventId = UUID.randomUUID();
        when(scheduleOutboxEventCommandRepository.findById(outboxEventId)).thenReturn(Optional.of(event));
        when(scheduleOutboxEventCommandRepository.save(event)).thenReturn(event);

        // when — MAX_RETRY_COUNT(3)번 연속 실패
        scheduleOutboxCommandService.recordFailure(outboxEventId, "1차 실패");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

        scheduleOutboxCommandService.recordFailure(outboxEventId, "2차 실패");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

        scheduleOutboxCommandService.recordFailure(outboxEventId, "3차 실패");

        // then
        assertThat(event.getRetryCount()).isEqualTo(ScheduleOutboxEvent.MAX_RETRY_COUNT);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getLastErrorMessage()).isEqualTo("3차 실패");
    }

    @Test
    void recordFailure는_존재하지_않는_이벤트면_예외를_던진다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        when(scheduleOutboxEventCommandRepository.findById(outboxEventId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleOutboxCommandService.recordFailure(outboxEventId, "boom"))
                .isInstanceOf(IllegalStateException.class);
    }
}
