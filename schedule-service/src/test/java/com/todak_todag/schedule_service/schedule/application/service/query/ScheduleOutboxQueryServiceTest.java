package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.ScheduleOutboxEventResult;
import com.todak_todag.schedule_service.schedule.domain.entity.OutboxEventStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ScheduleOutboxEventQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleOutboxQueryServiceTest {

    @Mock
    private ScheduleOutboxEventQueryRepository scheduleOutboxEventQueryRepository;

    @InjectMocks
    private ScheduleOutboxQueryService scheduleOutboxQueryService;

    @Test
    void findPending은_PENDING_상태의_이벤트를_Result로_매핑해_반환한다() {
        // given
        ScheduleOutboxEvent event = ScheduleOutboxEvent.create("ProviderReMatched", UUID.randomUUID(), "{}");
        when(scheduleOutboxEventQueryRepository.findByStatus(OutboxEventStatus.PENDING, 100))
                .thenReturn(List.of(event));

        // when
        List<ScheduleOutboxEventResult> results = scheduleOutboxQueryService.findPending(100);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).eventType()).isEqualTo("ProviderReMatched");
        assertThat(results.get(0).aggregateId()).isEqualTo(event.getAggregateId());
        assertThat(results.get(0).payload()).isEqualTo("{}");
        verify(scheduleOutboxEventQueryRepository).findByStatus(OutboxEventStatus.PENDING, 100);
    }

    @Test
    void findPending은_대기중인_이벤트가_없으면_빈_리스트를_반환한다() {
        // given
        when(scheduleOutboxEventQueryRepository.findByStatus(OutboxEventStatus.PENDING, 100))
                .thenReturn(List.of());

        // when
        List<ScheduleOutboxEventResult> results = scheduleOutboxQueryService.findPending(100);

        // then
        assertThat(results).isEmpty();
    }
}
