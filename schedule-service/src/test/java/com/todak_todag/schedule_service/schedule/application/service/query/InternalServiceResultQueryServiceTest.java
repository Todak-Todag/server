package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.result.InternalServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void 존재하는_결과를_조회하면_carePlanId와_serviceResultId를_반환한다() {
        // given
        UUID serviceResultId = UUID.randomUUID();
        UUID carePlanId = UUID.randomUUID();

        when(carePlanServiceResultQueryRepository.findCarePlanIdByServiceResultId(serviceResultId))
                .thenReturn(Optional.of(carePlanId));

        // when
        InternalServiceResultDetailResult result = internalServiceResultQueryService.findById(serviceResultId);

        // then
        assertThat(result.carePlanId()).isEqualTo(carePlanId);
        assertThat(result.serviceResultId()).isEqualTo(serviceResultId);
    }

    @Test
    void 존재하지_않는_ID로_조회하면_SERVICE_RESULTS_NOT_FOUND_예외가_발생한다() {
        // given
        UUID serviceResultId = UUID.randomUUID();
        when(carePlanServiceResultQueryRepository.findCarePlanIdByServiceResultId(serviceResultId))
                .thenReturn(Optional.empty());

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
        when(carePlanServiceResultQueryRepository.findCarePlanIdByServiceResultId(serviceResultId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalServiceResultQueryService.findById(serviceResultId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_RESULTS_NOT_FOUND);
    }

    // 조인 대상 일정이 유효하지 않으면 carePlanId를 확보할 수 없어 존재 검증 실패와 동일하게 취급
    @Test
    void 연결된_일정이_유효하지_않으면_SERVICE_RESULTS_NOT_FOUND_예외가_발생한다() {
        // given
        UUID serviceResultId = UUID.randomUUID();
        when(carePlanServiceResultQueryRepository.findCarePlanIdByServiceResultId(serviceResultId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalServiceResultQueryService.findById(serviceResultId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_RESULTS_NOT_FOUND);
    }
}
