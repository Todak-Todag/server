package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.MatchingAttemptRetryCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.query.MatchingAttemptSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptRetryResult;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceMatchingAttemptResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceMatchingAttemptCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceMatchingAttemptQueryService;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
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
import java.util.Optional;
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

    @Mock
    private ServiceMatchingAttemptCommandService serviceMatchingAttemptCommandService;

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

    @Nested
    @DisplayName("재매칭 시도 유스케이스 조합")
    class retryTest {

        private MatchingAttemptRetryCommand retryCommand(UUID matchingAttemptId, UUID requesterId) {
            return new MatchingAttemptRetryCommand(
                    matchingAttemptId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING,
                    requesterId
            );
        }

        @Test
        @DisplayName("대상 매칭 시도의 servicePreferenceId로 Care Plan을 조회해 커맨드에 넘긴다")
        void servicePreferenceId로_CarePlan을_조회해_넘긴다() {
            // given
            UUID matchingAttemptId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            CarePlanPort.CarePlanRange carePlanRange =
                    new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.of(2026, 9, 30), requesterId);

            MatchingAttemptRetryCommand retryCommand = retryCommand(matchingAttemptId, requesterId);

            when(serviceMatchingAttemptQueryService.findById(matchingAttemptId))
                    .thenReturn(Optional.of(new ServiceMatchingAttemptResult(matchingAttemptId, servicePreferenceId)));
            when(carePlanPort.findCarePlanRange(servicePreferenceId)).thenReturn(carePlanRange);
            when(serviceMatchingAttemptCommandService.retry(retryCommand, carePlanRange))
                    .thenReturn(new MatchingAttemptRetryResult(
                            matchingAttemptId,
                            servicePreferenceId,
                            retryCommand.date(),
                            PreferredTimeSlot.MORNING
                    ));

            // when
            MatchingAttemptRetryResult result = serviceMatchingAttemptFacade.retry(retryCommand);

            // then
            verify(carePlanPort).findCarePlanRange(servicePreferenceId);
            verify(serviceMatchingAttemptCommandService).retry(retryCommand, carePlanRange);
            assertThat(result.matchingAttemptId()).isEqualTo(matchingAttemptId);
        }

        @Test
        @DisplayName("존재하지 않는 매칭 시도면 403을 던지고 Internal API를 호출하지 않는다")
        void 존재하지_않으면_403을_던진다() {
            // given
            UUID matchingAttemptId = UUID.randomUUID();
            MatchingAttemptRetryCommand retryCommand = retryCommand(matchingAttemptId, UUID.randomUUID());

            when(serviceMatchingAttemptQueryService.findById(matchingAttemptId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> serviceMatchingAttemptFacade.retry(retryCommand))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(carePlanPort, never()).findCarePlanRange(any());
            verify(serviceMatchingAttemptCommandService, never()).retry(any(), any());
        }
    }
}
