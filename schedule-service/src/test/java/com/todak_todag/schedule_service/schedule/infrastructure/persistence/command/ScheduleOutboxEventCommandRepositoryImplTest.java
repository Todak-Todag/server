package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleOutboxEventCommandRepositoryImplTest {

    @Mock
    private SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    @InjectMocks
    private ScheduleOutboxEventCommandRepositoryImpl scheduleOutboxEventCommandRepositoryImpl;

    @Test
    void save를_호출하면_SpringDataScheduleOutboxEventRepository_save로_위임한다() {
        // given
        ScheduleOutboxEvent event = ScheduleOutboxEvent.create("ProviderReMatched", UUID.randomUUID(), "{}");
        when(springDataScheduleOutboxEventRepository.save(event)).thenReturn(event);

        // when
        ScheduleOutboxEvent result = scheduleOutboxEventCommandRepositoryImpl.save(event);

        // then
        verify(springDataScheduleOutboxEventRepository).save(event);
        assertThat(result).isSameAs(event);
    }

    @Test
    void findById를_호출하면_SpringDataScheduleOutboxEventRepository_findById로_위임한다() {
        // given
        ScheduleOutboxEvent event = ScheduleOutboxEvent.create("ProviderReMatched", UUID.randomUUID(), "{}");
        UUID outboxEventId = UUID.randomUUID();
        when(springDataScheduleOutboxEventRepository.findById(outboxEventId)).thenReturn(Optional.of(event));

        // when
        Optional<ScheduleOutboxEvent> result = scheduleOutboxEventCommandRepositoryImpl.findById(outboxEventId);

        // then
        assertThat(result).contains(event);
    }

    @Test
    void findById는_존재하지_않으면_빈_Optional을_반환한다() {
        // given
        UUID outboxEventId = UUID.randomUUID();
        when(springDataScheduleOutboxEventRepository.findById(outboxEventId)).thenReturn(Optional.empty());

        // when
        Optional<ScheduleOutboxEvent> result = scheduleOutboxEventCommandRepositoryImpl.findById(outboxEventId);

        // then
        assertThat(result).isEmpty();
    }
}
