package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.query.MatchingAttemptSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceMatchingAttemptQueryService;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceMatchingAttemptFacadeTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CarePlanPort carePlanPort;

    @Mock
    private ServiceMatchingAttemptQueryService serviceMatchingAttemptQueryService;

    @InjectMocks
    private ServiceMatchingAttemptFacade serviceMatchingAttemptFacade;

    private CarePlanPort.CarePlanSummary carePlan(CarePlanPort.CarePlanStatus status) {
        return new CarePlanPort.CarePlanSummary(UUID.randomUUID(), status);
    }

    private MatchingAttemptSearchResult sampleResult(UUID servicePreferenceId) {
        return new MatchingAttemptSearchResult(
                UUID.randomUUID(),
                servicePreferenceId,
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 1),
                null,
                MatchingAttemptStatus.FAILED,
                "해당 날짜/시간대에 제공 가능한 서비스 제공자 없음",
                Instant.parse("2026-09-01T09:00:00Z"),
                null
        );
    }

    @Nested
    @DisplayName("Care Plan 상태 게이트")
    class carePlanStatusTest {

        @Test
        void Care_Plan이_CONFIRMED면_담당_servicePreferenceId_목록으로_조회한다() {
            // given
            UUID userId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            List<UUID> servicePreferenceIds = List.of(servicePreferenceId);

            when(carePlanPort.findCarePlanByPatient(userId))
                    .thenReturn(carePlan(CarePlanPort.CarePlanStatus.CONFIRMED));
            when(carePlanPort.findServicePreferenceIds(userId)).thenReturn(servicePreferenceIds);
            when(serviceMatchingAttemptQueryService.search(any(), any(), anyBoolean(), any()))
                    .thenReturn(new PageImpl<>(List.of(sampleResult(servicePreferenceId)), PAGEABLE, 1));

            MatchingAttemptSearchQuery searchQuery =
                    MatchingAttemptSearchQuery.of(userId, null, PAGEABLE);

            // when
            Page<MatchingAttemptSearchResult> result = serviceMatchingAttemptFacade.search(searchQuery);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(serviceMatchingAttemptQueryService).search(
                    eq(servicePreferenceIds),
                    eq(MatchingAttemptStatus.FAILED),
                    eq(true),
                    eq(PAGEABLE)
            );
        }

        @Test
        void Care_Plan이_CONFIRMED가_아니면_DB_조회_없이_빈_페이지를_반환한다() {
            // given
            UUID userId = UUID.randomUUID();

            when(carePlanPort.findCarePlanByPatient(userId))
                    .thenReturn(carePlan(CarePlanPort.CarePlanStatus.IN_PROGRESS));

            MatchingAttemptSearchQuery searchQuery =
                    MatchingAttemptSearchQuery.of(userId, null, PAGEABLE);

            // when
            Page<MatchingAttemptSearchResult> result = serviceMatchingAttemptFacade.search(searchQuery);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(carePlanPort, never()).findServicePreferenceIds(any());
            verify(serviceMatchingAttemptQueryService, never()).search(any(), any(), anyBoolean(), any());
        }

        @Test
        void Care_Plan_조회가_실패하면_예외를_그대로_전파한다() {
            // given
            UUID userId = UUID.randomUUID();

            when(carePlanPort.findCarePlanByPatient(userId))
                    .thenThrow(new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));

            MatchingAttemptSearchQuery searchQuery =
                    MatchingAttemptSearchQuery.of(userId, null, PAGEABLE);

            // when & then
            assertThatThrownBy(() -> serviceMatchingAttemptFacade.search(searchQuery))
                    .isInstanceOf(BusinessException.class);

            verify(serviceMatchingAttemptQueryService, never()).search(any(), any(), anyBoolean(), any());
        }
    }

    @Nested
    @DisplayName("소유권 필터링")
    class ownershipTest {

        @Test
        void 담당하는_servicePreferenceId가_없으면_DB_조회_없이_빈_페이지를_반환한다() {
            // given
            UUID userId = UUID.randomUUID();

            when(carePlanPort.findCarePlanByPatient(userId))
                    .thenReturn(carePlan(CarePlanPort.CarePlanStatus.CONFIRMED));
            when(carePlanPort.findServicePreferenceIds(userId)).thenReturn(List.of());

            MatchingAttemptSearchQuery searchQuery =
                    MatchingAttemptSearchQuery.of(userId, null, PAGEABLE);

            // when
            Page<MatchingAttemptSearchResult> result = serviceMatchingAttemptFacade.search(searchQuery);

            // then
            assertThat(result.getContent()).isEmpty();
            verify(serviceMatchingAttemptQueryService, never()).search(any(), any(), anyBoolean(), any());
        }

        @Test
        void Internal_API가_반환한_ID_목록을_그대로_조회_조건으로_넘긴다() {
            // given
            UUID userId = UUID.randomUUID();
            List<UUID> servicePreferenceIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            when(carePlanPort.findCarePlanByPatient(userId))
                    .thenReturn(carePlan(CarePlanPort.CarePlanStatus.CONFIRMED));
            when(carePlanPort.findServicePreferenceIds(userId)).thenReturn(servicePreferenceIds);
            when(serviceMatchingAttemptQueryService.search(any(), any(), anyBoolean(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

            MatchingAttemptSearchQuery searchQuery =
                    MatchingAttemptSearchQuery.of(userId, null, PAGEABLE);

            // when
            serviceMatchingAttemptFacade.search(searchQuery);

            // then
            verify(serviceMatchingAttemptQueryService).search(
                    eq(servicePreferenceIds), any(), anyBoolean(), any()
            );
        }
    }

    @Nested
    @DisplayName("status 필터 전달")
    class statusFilterTest {

        @Test
        void MATCHED_조회는_일정_미생성_조건_없이_넘긴다() {
            // given
            UUID userId = UUID.randomUUID();
            List<UUID> servicePreferenceIds = List.of(UUID.randomUUID());

            when(carePlanPort.findCarePlanByPatient(userId))
                    .thenReturn(carePlan(CarePlanPort.CarePlanStatus.CONFIRMED));
            when(carePlanPort.findServicePreferenceIds(userId)).thenReturn(servicePreferenceIds);
            when(serviceMatchingAttemptQueryService.search(any(), any(), anyBoolean(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

            MatchingAttemptSearchQuery searchQuery =
                    MatchingAttemptSearchQuery.of(userId, MatchingAttemptStatus.MATCHED, PAGEABLE);

            // when
            serviceMatchingAttemptFacade.search(searchQuery);

            // then
            verify(serviceMatchingAttemptQueryService).search(
                    eq(servicePreferenceIds),
                    eq(MatchingAttemptStatus.MATCHED),
                    eq(false),
                    eq(PAGEABLE)
            );
        }
    }
}
