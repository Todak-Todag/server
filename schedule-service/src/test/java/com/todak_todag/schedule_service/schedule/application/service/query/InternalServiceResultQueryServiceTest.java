package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.result.InternalServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalServiceResultQueryServiceTest {

    @Mock
    private CarePlanServiceResultQueryRepository carePlanServiceResultQueryRepository;

    @InjectMocks
    private InternalServiceResultQueryService internalServiceResultQueryService;

    @Test
    void 존재하는_결과를_조회하면_Result로_매핑하여_반환한다() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime finishedAt = LocalDateTime.now().minusHours(1);
        CarePlanServiceResult carePlanServiceResult = CarePlanServiceResult.record(
                serviceScheduleId, startedAt, finishedAt, "비고"
        );

        when(carePlanServiceResultQueryRepository.findById(carePlanServiceResult.getServiceResultId()))
                .thenReturn(Optional.of(carePlanServiceResult));

        // when
        InternalServiceResultDetailResult result = internalServiceResultQueryService.findById(carePlanServiceResult.getServiceResultId());

        // then
        assertThat(result.serviceResultId()).isEqualTo(carePlanServiceResult.getServiceResultId());
        assertThat(result.serviceScheduleId()).isEqualTo(serviceScheduleId);
        assertThat(result.startedAt()).isEqualTo(startedAt);
        assertThat(result.finishedAt()).isEqualTo(finishedAt);
    }

    @Test
    void 존재하지_않는_ID로_조회하면_SERVICE_RESULTS_NOT_FOUND_예외가_발생한다() {
        // given
        UUID serviceResultId = UUID.randomUUID();
        when(carePlanServiceResultQueryRepository.findById(serviceResultId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalServiceResultQueryService.findById(serviceResultId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_RESULTS_NOT_FOUND);
    }

    @Test
    void 논리_삭제된_ID로_조회하면_SERVICE_RESULTS_NOT_FOUND_예외가_발생한다() {
        // given
        UUID serviceResultId = UUID.randomUUID();
        when(carePlanServiceResultQueryRepository.findById(serviceResultId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalServiceResultQueryService.findById(serviceResultId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_RESULTS_NOT_FOUND);
    }
}
