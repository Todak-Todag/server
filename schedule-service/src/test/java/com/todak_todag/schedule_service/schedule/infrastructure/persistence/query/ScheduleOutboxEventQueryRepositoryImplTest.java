package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.schedule.domain.entity.OutboxEventStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleOutboxEventQueryRepositoryImplTest {

    @Mock
    private SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    @InjectMocks
    private ScheduleOutboxEventQueryRepositoryImpl scheduleOutboxEventQueryRepositoryImpl;

    @Test
    void findByStatus는_생성_순서로_최대_limit건을_조회한다() {
        // given
        ScheduleOutboxEvent event = ScheduleOutboxEvent.create("ProviderReMatched", UUID.randomUUID(), "{}");
        when(springDataScheduleOutboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxEventStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));

        // when
        List<ScheduleOutboxEvent> result = scheduleOutboxEventQueryRepositoryImpl.findByStatus(OutboxEventStatus.PENDING, 50);

        // then
        assertThat(result).containsExactly(event);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(springDataScheduleOutboxEventRepository)
                .findByStatusOrderByCreatedAtAsc(eq(OutboxEventStatus.PENDING), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }
}
