package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarePlanServiceResultCommandRepositoryImplTest {

    @Mock
    private SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @InjectMocks
    private CarePlanServiceResultCommandRepositoryImpl carePlanServiceResultCommandRepositoryImpl;

    @Test
    void save를_호출하면_SpringDataCarePlanServiceResultRepository_save로_위임한다() {
        // given
        CarePlanServiceResult result = CarePlanServiceResult.record(
                UUID.randomUUID(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), "비고"
        );
        when(springDataCarePlanServiceResultRepository.save(result)).thenReturn(result);

        // when
        CarePlanServiceResult saved = carePlanServiceResultCommandRepositoryImpl.save(result);

        // then
        verify(springDataCarePlanServiceResultRepository).save(result);
        assertThat(saved).isSameAs(result);
    }

    @Test
    void existsByServiceScheduleId를_호출하면_SpringData의_소프트삭제_제외_조회로_위임한다() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        when(springDataCarePlanServiceResultRepository.existsByServiceScheduleIdAndDeletedAtIsNull(serviceScheduleId))
                .thenReturn(true);

        // when
        boolean exists = carePlanServiceResultCommandRepositoryImpl.existsByServiceScheduleId(serviceScheduleId);

        // then
        assertThat(exists).isTrue();
        verify(springDataCarePlanServiceResultRepository).existsByServiceScheduleIdAndDeletedAtIsNull(serviceScheduleId);
    }
}
