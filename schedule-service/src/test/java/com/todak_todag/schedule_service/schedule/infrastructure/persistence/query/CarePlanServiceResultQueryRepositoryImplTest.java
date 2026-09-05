package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarePlanServiceResultQueryRepositoryImplTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @InjectMocks
    private CarePlanServiceResultQueryRepositoryImpl carePlanServiceResultQueryRepositoryImpl;

    @Test
    void findById를_호출하면_SpringData의_소프트삭제_제외_조회로_위임한다() {
        // given
        UUID serviceResultId = UUID.randomUUID();
        CarePlanServiceResult result = CarePlanServiceResult.record(
                UUID.randomUUID(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), "비고"
        );
        when(springDataCarePlanServiceResultRepository.findByServiceResultIdAndDeletedAtIsNull(serviceResultId))
                .thenReturn(Optional.of(result));

        // when
        Optional<CarePlanServiceResult> found = carePlanServiceResultQueryRepositoryImpl.findById(serviceResultId);

        // then
        assertThat(found).contains(result);
        verify(springDataCarePlanServiceResultRepository).findByServiceResultIdAndDeletedAtIsNull(serviceResultId);
    }

    @Test
    void 존재하지_않으면_빈_Optional을_반환한다() {
        // given
        UUID serviceResultId = UUID.randomUUID();
        when(springDataCarePlanServiceResultRepository.findByServiceResultIdAndDeletedAtIsNull(serviceResultId))
                .thenReturn(Optional.empty());

        // when
        Optional<CarePlanServiceResult> found = carePlanServiceResultQueryRepositoryImpl.findById(serviceResultId);

        // then
        assertThat(found).isEmpty();
    }
}
