package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceScheduleCommandRepositoryImplTest {

    @Mock
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @InjectMocks
    private ServiceScheduleCommandRepositoryImpl serviceScheduleCommandRepositoryImpl;

    @Test
    void save를_호출하면_SpringDataServiceScheduleRepository_save로_위임한다() {
        // given
        LocalDate date = LocalDate.now().plusDays(1);
        ServiceSchedule schedule = ServiceSchedule.confirm(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), date, date.atTime(9, 0), date.atTime(10, 0)
        );
        when(springDataServiceScheduleRepository.save(schedule)).thenReturn(schedule);

        // when
        ServiceSchedule result = serviceScheduleCommandRepositoryImpl.save(schedule);

        // then
        verify(springDataServiceScheduleRepository).save(schedule);
        assertThat(result).isSameAs(schedule);
    }
}
