package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompleteCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompletionStatus;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.query.ServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceScheduleCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceScheduleFacadeTest {

    @Mock
    private ServiceScheduleQueryService serviceScheduleQueryService;

    @Mock
    private CarePlanPort carePlanPort;

    @Mock
    private ProviderOfferingPort providerOfferingPort;

    @Mock
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @InjectMocks
    private ServiceScheduleFacade serviceScheduleFacade;

    @Nested
    @DisplayName("서비스 일정 변경")
    class rescheduleTest {
        @Test
        @DisplayName("일정을 조회하고 Care Plan을 조회한 뒤 CommandService에 위임한다")
        void reschedule_success_delegatesToCommandService() {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            LocalDate requestedDate = LocalDate.now().plusDays(2);

            ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(serviceScheduleId, requestedDate, requesterId);
            ServiceScheduleResult scheduleResult = new ServiceScheduleResult(serviceScheduleId, servicePreferenceId, UUID.randomUUID());
            CarePlanPort.CarePlanRange carePlanRange =
                    new CarePlanPort.CarePlanRange(UUID.randomUUID(), requestedDate.plusDays(10), requesterId);
            ServiceScheduleRescheduleResult expected = new ServiceScheduleRescheduleResult(serviceScheduleId, ScheduleStatus.RESCHEDULING);

            when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.of(scheduleResult));
            when(carePlanPort.findCarePlanRange(servicePreferenceId)).thenReturn(carePlanRange);
            when(serviceScheduleCommandService.reschedule(command, carePlanRange)).thenReturn(expected);

            // when
            ServiceScheduleRescheduleResult result = serviceScheduleFacade.reschedule(command);

            // then
            assertThat(result).isEqualTo(expected);
            verify(serviceScheduleCommandService).reschedule(eq(command), eq(carePlanRange));
        }

        @Test
        @DisplayName("존재하지 않는 일정이면 403을 던지고 Care Plan을 조회하지 않는다 (리소스 존재 비노출)")
        void reschedule_notFound_forbiddenWithoutCarePlanLookup() {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(
                    serviceScheduleId, LocalDate.now().plusDays(1), UUID.randomUUID()
            );
            when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> serviceScheduleFacade.reschedule(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(carePlanPort, never()).findCarePlanRange(any());
            verify(serviceScheduleCommandService, never()).reschedule(any(), any());
        }
    }

    @Nested
    @DisplayName("서비스 일정 취소")
    class cancelTest {
        @Test
        @DisplayName("일정을 조회하고 Care Plan을 조회한 뒤 CommandService에 위임한다")
        void cancel_success_delegatesToCommandService() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(UUID.randomUUID(), "개인 사정", requesterId);
            ServiceScheduleResult scheduleResult = new ServiceScheduleResult(command.serviceScheduleId(), servicePreferenceId, UUID.randomUUID());
            CarePlanPort.CarePlanRange carePlanRange =
                    new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.now().plusDays(10), requesterId);
            ServiceScheduleCancelResult expected = new ServiceScheduleCancelResult(command.serviceScheduleId(), LocalDateTime.now());

            when(serviceScheduleQueryService.findById(command.serviceScheduleId())).thenReturn(Optional.of(scheduleResult));
            when(carePlanPort.findCarePlanRange(servicePreferenceId)).thenReturn(carePlanRange);
            when(serviceScheduleCommandService.cancel(command, carePlanRange)).thenReturn(expected);

            // when
            ServiceScheduleCancelResult result = serviceScheduleFacade.cancel(command);

            // then
            assertThat(result).isEqualTo(expected);
            verify(serviceScheduleCommandService).cancel(command, carePlanRange);
        }

        @Test
        @DisplayName("존재하지 않는 일정이면 403을 던지고 Care Plan을 조회하지 않는다 (리소스 존재 비노출)")
        void cancel_notFound_forbiddenWithoutCarePlanLookup() {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(serviceScheduleId, "개인 사정", UUID.randomUUID());
            when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> serviceScheduleFacade.cancel(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(carePlanPort, never()).findCarePlanRange(any());
            verify(serviceScheduleCommandService, never()).cancel(any(), any());
        }
    }

    @Nested
    @DisplayName("서비스 수행 완료")
    class completeTest {
        @Test
        @DisplayName("일정을 조회하고 배정된 providerId를 조회한 뒤 CommandService에 위임한다")
        void complete_success_delegatesToCommandService() {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            UUID serviceOfferingId = UUID.randomUUID();
            UUID providerId = UUID.randomUUID();
            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(serviceScheduleId, ServiceScheduleCompletionStatus.COMPLETED, providerId);
            ServiceScheduleResult scheduleResult = new ServiceScheduleResult(serviceScheduleId, UUID.randomUUID(), serviceOfferingId);
            ServiceScheduleCompleteResult expected = new ServiceScheduleCompleteResult(serviceScheduleId, ScheduleStatus.COMPLETED);

            when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.of(scheduleResult));
            when(providerOfferingPort.findAssignedProviderId(serviceOfferingId)).thenReturn(providerId);
            when(serviceScheduleCommandService.complete(command, providerId)).thenReturn(expected);

            // when
            ServiceScheduleCompleteResult result = serviceScheduleFacade.complete(command);

            // then
            assertThat(result).isEqualTo(expected);
            verify(serviceScheduleCommandService).complete(command, providerId);
        }

        @Test
        @DisplayName("존재하지 않는 일정이면 403을 던지고 provider 조회를 하지 않는다 (리소스 존재 비노출)")
        void complete_notFound_forbiddenWithoutProviderLookup() {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(serviceScheduleId, ServiceScheduleCompletionStatus.COMPLETED, UUID.randomUUID());
            when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> serviceScheduleFacade.complete(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(providerOfferingPort, never()).findAssignedProviderId(any());
            verify(serviceScheduleCommandService, never()).complete(any(), any());
        }
    }

    @Nested
    @DisplayName("서비스 일정 목록 조회")
    class searchTest {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("퇴원 예정자면 care-plan-service에서 servicePreferenceId 목록을 조회해 QueryService에 위임한다")
        void search_patient_delegatesWithServicePreferenceIds() {
            // given
            UUID patientId = UUID.randomUUID();
            List<UUID> servicePreferenceIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            ServiceScheduleSearchQuery query = new ServiceScheduleSearchQuery(patientId, UserRole.PATIENT, null, null, pageable);
            Page<ServiceScheduleSearchResult> expected = new PageImpl<>(List.of());

            when(carePlanPort.findServicePreferenceIds(patientId)).thenReturn(servicePreferenceIds);
            when(serviceScheduleQueryService.search(servicePreferenceIds, null, null, null, pageable)).thenReturn(expected);

            // when
            Page<ServiceScheduleSearchResult> result = serviceScheduleFacade.search(query);

            // then
            assertThat(result).isEqualTo(expected);
            verify(serviceScheduleQueryService).search(servicePreferenceIds, null, null, null, pageable);
            verify(providerOfferingPort, never()).findServiceOfferingIds(any());
        }

        @Test
        @DisplayName("서비스 제공자면 provider-service에서 serviceOfferingId 목록을 조회해 QueryService에 위임한다")
        void search_serviceProvider_delegatesWithServiceOfferingIds() {
            // given
            UUID providerId = UUID.randomUUID();
            List<UUID> serviceOfferingIds = List.of(UUID.randomUUID());
            ServiceScheduleSearchQuery query = new ServiceScheduleSearchQuery(providerId, UserRole.SERVICE_PROVIDER, ScheduleStatus.SCHEDULED, null, pageable);
            Page<ServiceScheduleSearchResult> expected = new PageImpl<>(List.of());

            when(providerOfferingPort.findServiceOfferingIds(providerId)).thenReturn(serviceOfferingIds);
            when(serviceScheduleQueryService.search(null, serviceOfferingIds, ScheduleStatus.SCHEDULED, null, pageable)).thenReturn(expected);

            // when
            Page<ServiceScheduleSearchResult> result = serviceScheduleFacade.search(query);

            // then
            assertThat(result).isEqualTo(expected);
            verify(serviceScheduleQueryService).search(null, serviceOfferingIds, ScheduleStatus.SCHEDULED, null, pageable);
            verify(carePlanPort, never()).findServicePreferenceIds(any());
        }

        @Test
        @DisplayName("퇴원 예정자가 담당하는 servicePreferenceId가 하나도 없으면 DB 조회 없이 빈 페이지를 반환한다")
        void search_patient_emptyIds_returnsEmptyPageWithoutQuery() {
            // given
            UUID patientId = UUID.randomUUID();
            ServiceScheduleSearchQuery query = new ServiceScheduleSearchQuery(patientId, UserRole.PATIENT, null, null, pageable);

            when(carePlanPort.findServicePreferenceIds(patientId)).thenReturn(List.of());

            // when
            Page<ServiceScheduleSearchResult> result = serviceScheduleFacade.search(query);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(serviceScheduleQueryService, never()).search(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("서비스 제공자가 담당하는 serviceOfferingId가 하나도 없으면 DB 조회 없이 빈 페이지를 반환한다")
        void search_serviceProvider_emptyIds_returnsEmptyPageWithoutQuery() {
            // given
            UUID providerId = UUID.randomUUID();
            ServiceScheduleSearchQuery query = new ServiceScheduleSearchQuery(providerId, UserRole.SERVICE_PROVIDER, null, null, pageable);

            when(providerOfferingPort.findServiceOfferingIds(providerId)).thenReturn(List.of());

            // when
            Page<ServiceScheduleSearchResult> result = serviceScheduleFacade.search(query);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(serviceScheduleQueryService, never()).search(any(), any(), any(), any(), any());
        }
    }
}
