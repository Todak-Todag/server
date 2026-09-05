package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultDetailQuery;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultSearchResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceResultCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceResultQueryService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
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
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceResultFacadeTest {

    @Mock
    private ServiceScheduleQueryService serviceScheduleQueryService;

    @Mock
    private ProviderOfferingPort providerOfferingPort;

    @Mock
    private ServiceResultCommandService serviceResultCommandService;

    @Mock
    private CarePlanPort carePlanPort;

    @Mock
    private ServiceResultQueryService serviceResultQueryService;

    @InjectMocks
    private ServiceResultFacade serviceResultFacade;

    @Test
    @DisplayName("일정을 조회하고 배정된 providerId를 조회한 뒤 CommandService에 위임한다")
    void register_success_delegatesToCommandService() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                serviceScheduleId, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), "비고", providerId
        );
        ServiceScheduleResult scheduleResult = new ServiceScheduleResult(serviceScheduleId, UUID.randomUUID(), serviceOfferingId);
        ServiceResultRegisterResult expected = new ServiceResultRegisterResult(UUID.randomUUID());

        when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.of(scheduleResult));
        when(providerOfferingPort.findAssignedProviderId(serviceOfferingId)).thenReturn(providerId);
        when(serviceResultCommandService.register(command, providerId)).thenReturn(expected);

        // when
        ServiceResultRegisterResult result = serviceResultFacade.register(command);

        // then
        assertThat(result).isEqualTo(expected);
        verify(serviceResultCommandService).register(command, providerId);
    }

    @Test
    @DisplayName("존재하지 않는 일정이면 403을 던지고 provider 조회를 하지 않는다")
    void register_notFound_forbiddenWithoutProviderLookup() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                serviceScheduleId, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), null, UUID.randomUUID()
        );
        when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serviceResultFacade.register(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

        verify(providerOfferingPort, never()).findAssignedProviderId(any());
        verify(serviceResultCommandService, never()).register(any(), any());
    }

    @Nested
    @DisplayName("서비스 수행 결과 목록 조회")
    class searchTest {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("퇴원 예정자면 care-plan-service에서 servicePreferenceId 목록을 조회해 QueryService에 위임한다")
        void search_patient_delegatesWithServicePreferenceIds() {
            // given
            UUID patientId = UUID.randomUUID();
            List<UUID> servicePreferenceIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            ServiceResultSearchQuery query = new ServiceResultSearchQuery(patientId, UserRole.PATIENT, pageable);
            Page<ServiceResultSearchResult> expected = new PageImpl<>(List.of(
                    new ServiceResultSearchResult(
                            UUID.randomUUID(),
                            LocalDateTime.of(2026, 9, 1, 9, 0),
                            LocalDateTime.of(2026, 9, 1, 10, 0)
                    )
            ), pageable, 1);

            when(carePlanPort.findServicePreferenceIds(patientId)).thenReturn(servicePreferenceIds);
            when(serviceResultQueryService.search(servicePreferenceIds, null, pageable)).thenReturn(expected);

            // when
            Page<ServiceResultSearchResult> result = serviceResultFacade.search(query);

            // then
            assertThat(result).isEqualTo(expected);
            verify(serviceResultQueryService).search(servicePreferenceIds, null, pageable);
            verify(providerOfferingPort, never()).findServiceOfferingIds(any());
        }

        @Test
        @DisplayName("서비스 제공자면 provider-service에서 serviceOfferingId 목록을 조회해 QueryService에 위임한다")
        void search_serviceProvider_delegatesWithServiceOfferingIds() {
            // given
            UUID providerId = UUID.randomUUID();
            List<UUID> serviceOfferingIds = List.of(UUID.randomUUID());
            ServiceResultSearchQuery query = new ServiceResultSearchQuery(providerId, UserRole.SERVICE_PROVIDER, pageable);
            Page<ServiceResultSearchResult> expected = new PageImpl<>(List.of(), pageable, 0);

            when(providerOfferingPort.findServiceOfferingIds(providerId)).thenReturn(serviceOfferingIds);
            when(serviceResultQueryService.search(null, serviceOfferingIds, pageable)).thenReturn(expected);

            // when
            Page<ServiceResultSearchResult> result = serviceResultFacade.search(query);

            // then
            assertThat(result).isEqualTo(expected);
            verify(serviceResultQueryService).search(null, serviceOfferingIds, pageable);
            verify(carePlanPort, never()).findServicePreferenceIds(any());
        }

        @Test
        @DisplayName("퇴원 예정자가 담당하는 servicePreferenceId가 하나도 없으면 DB 조회 없이 빈 페이지를 반환한다")
        void search_patient_emptyIds_returnsEmptyPageWithoutQuery() {
            // given
            UUID patientId = UUID.randomUUID();
            ServiceResultSearchQuery query = new ServiceResultSearchQuery(patientId, UserRole.PATIENT, pageable);

            when(carePlanPort.findServicePreferenceIds(patientId)).thenReturn(List.of());

            // when
            Page<ServiceResultSearchResult> result = serviceResultFacade.search(query);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(serviceResultQueryService, never()).search(any(), any(), any());
        }

        @Test
        @DisplayName("서비스 제공자가 담당하는 serviceOfferingId가 하나도 없으면 DB 조회 없이 빈 페이지를 반환한다")
        void search_serviceProvider_emptyIds_returnsEmptyPageWithoutQuery() {
            // given
            UUID providerId = UUID.randomUUID();
            ServiceResultSearchQuery query = new ServiceResultSearchQuery(providerId, UserRole.SERVICE_PROVIDER, pageable);

            when(providerOfferingPort.findServiceOfferingIds(providerId)).thenReturn(List.of());

            // when
            Page<ServiceResultSearchResult> result = serviceResultFacade.search(query);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(serviceResultQueryService, never()).search(any(), any(), any());
        }

        @Test
        @DisplayName("퇴원 예정자/서비스 제공자가 아닌 역할이면 403을 던진다")
        void search_otherRole_forbidden() {
            // given
            ServiceResultSearchQuery query = new ServiceResultSearchQuery(UUID.randomUUID(), UserRole.ADMIN, pageable);

            // when & then
            assertThatThrownBy(() -> serviceResultFacade.search(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(serviceResultQueryService, never()).search(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("서비스 수행 결과 상세 조회")
    class detailTest {

        private ServiceResultDetailResult detailResult(UUID serviceResultId, UUID serviceScheduleId, String note) {
            return new ServiceResultDetailResult(
                    serviceResultId,
                    serviceScheduleId,
                    LocalDateTime.of(2026, 9, 1, 9, 0),
                    LocalDateTime.of(2026, 9, 1, 10, 0),
                    note
            );
        }

        @Test
        @DisplayName("퇴원 예정자가 본인 소유 결과를 조회하면 정상 반환한다")
        void detail_patient_ownResult_success() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            UUID patientId = UUID.randomUUID();

            ServiceResultDetailResult expected = detailResult(serviceResultId, serviceScheduleId, "정상 수행");
            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, patientId, UserRole.PATIENT);

            when(serviceResultQueryService.findDetailById(serviceResultId)).thenReturn(Optional.of(expected));

            when(serviceScheduleQueryService.findById(serviceScheduleId))
                    .thenReturn(Optional.of(new ServiceScheduleResult(serviceScheduleId, servicePreferenceId, UUID.randomUUID())));
            when(carePlanPort.findCarePlanRange(servicePreferenceId))
                    .thenReturn(new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.of(2026, 9, 30), patientId));

            // when
            ServiceResultDetailResult result = serviceResultFacade.detail(query);

            // then
            assertThat(result).isEqualTo(expected);
            verify(carePlanPort).findCarePlanRange(servicePreferenceId);
            verify(providerOfferingPort, never()).findAssignedProviderId(any());
        }

        @Test
        @DisplayName("서비스 제공자가 본인 담당 결과를 조회하면 정상 반환한다")
        void detail_serviceProvider_ownResult_success() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            UUID serviceOfferingId = UUID.randomUUID();
            UUID providerId = UUID.randomUUID();

            ServiceResultDetailResult expected = detailResult(serviceResultId, serviceScheduleId, "정상 수행");
            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, providerId, UserRole.SERVICE_PROVIDER);

            when(serviceResultQueryService.findDetailById(serviceResultId)).thenReturn(Optional.of(expected));
            when(serviceScheduleQueryService.findById(serviceScheduleId))
                    .thenReturn(Optional.of(new ServiceScheduleResult(serviceScheduleId, UUID.randomUUID(), serviceOfferingId)));
            when(providerOfferingPort.findAssignedProviderId(serviceOfferingId)).thenReturn(providerId);

            // when
            ServiceResultDetailResult result = serviceResultFacade.detail(query);

            // then
            assertThat(result).isEqualTo(expected);
            verify(providerOfferingPort).findAssignedProviderId(serviceOfferingId);
            verify(carePlanPort, never()).findCarePlanRange(any());
        }

        @Test
        @DisplayName("note가 null인 결과도 그대로 반환한다")
        void detail_nullNote_returnedAsIs() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            UUID serviceOfferingId = UUID.randomUUID();
            UUID providerId = UUID.randomUUID();

            ServiceResultDetailResult expected = detailResult(serviceResultId, serviceScheduleId, null);
            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, providerId, UserRole.SERVICE_PROVIDER);

            when(serviceResultQueryService.findDetailById(serviceResultId)).thenReturn(Optional.of(expected));
            when(serviceScheduleQueryService.findById(serviceScheduleId))
                    .thenReturn(Optional.of(new ServiceScheduleResult(serviceScheduleId, UUID.randomUUID(), serviceOfferingId)));
            when(providerOfferingPort.findAssignedProviderId(serviceOfferingId)).thenReturn(providerId);

            // when
            ServiceResultDetailResult result = serviceResultFacade.detail(query);

            // then
            assertThat(result.note()).isNull();
            assertThat(result.serviceScheduleId()).isEqualTo(serviceScheduleId);
        }

        @Test
        @DisplayName("존재하지 않는 serviceResultId면 403을 던지고 일정 조회/Internal API 호출을 하지 않는다")
        void detail_notFound_throwsForbiddenWithoutLookup() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, UUID.randomUUID(), UserRole.PATIENT);

            when(serviceResultQueryService.findDetailById(serviceResultId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> serviceResultFacade.detail(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(serviceScheduleQueryService, never()).findById(any());
            verify(carePlanPort, never()).findCarePlanRange(any());
            verify(providerOfferingPort, never()).findAssignedProviderId(any());
        }

        @Test
        @DisplayName("존재하지 않는 결과와 본인 소유가 아닌 결과가 동일한 에러 코드를 던진다")
        void detail_notFoundAndNotOwner_throwSameErrorCode() {
            // given
            UUID missingResultId = UUID.randomUUID();
            UUID existingResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            ServiceResultDetailQuery missingQuery = new ServiceResultDetailQuery(missingResultId, requesterId, UserRole.PATIENT);
            ServiceResultDetailQuery notOwnerQuery = new ServiceResultDetailQuery(existingResultId, requesterId, UserRole.PATIENT);

            // 존재하지 않는 결과
            when(serviceResultQueryService.findDetailById(missingResultId)).thenReturn(Optional.empty());

            // 존재하지만 다른 환자 소유인 결과
            when(serviceResultQueryService.findDetailById(existingResultId))
                    .thenReturn(Optional.of(detailResult(existingResultId, serviceScheduleId, "정상 수행")));
            when(serviceScheduleQueryService.findById(serviceScheduleId))
                    .thenReturn(Optional.of(new ServiceScheduleResult(serviceScheduleId, servicePreferenceId, UUID.randomUUID())));
            when(carePlanPort.findCarePlanRange(servicePreferenceId))
                    .thenReturn(new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.of(2026, 9, 30), UUID.randomUUID()));

            // when
            BusinessException missingException = catchThrowableOfType(
                    () -> serviceResultFacade.detail(missingQuery), BusinessException.class
            );
            BusinessException notOwnerException = catchThrowableOfType(
                    () -> serviceResultFacade.detail(notOwnerQuery), BusinessException.class
            );

            // then
            assertThat(missingException.getErrorCode()).isEqualTo(notOwnerException.getErrorCode());
            assertThat(missingException.getErrorCode()).isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
        }

        @Test
        @DisplayName("퇴원 예정자가 본인 소유가 아닌 결과를 조회하면 403을 던진다")
        void detail_patient_notOwner_forbidden() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            UUID otherPatientId = UUID.randomUUID();

            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, requesterId, UserRole.PATIENT);

            when(serviceResultQueryService.findDetailById(serviceResultId))
                    .thenReturn(Optional.of(detailResult(serviceResultId, serviceScheduleId, "정상 수행")));
            when(serviceScheduleQueryService.findById(serviceScheduleId))
                    .thenReturn(Optional.of(new ServiceScheduleResult(serviceScheduleId, servicePreferenceId, UUID.randomUUID())));
            when(carePlanPort.findCarePlanRange(servicePreferenceId))
                    .thenReturn(new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.of(2026, 9, 30), otherPatientId));

            // when & then
            assertThatThrownBy(() -> serviceResultFacade.detail(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
        }

        @Test
        @DisplayName("서비스 제공자가 본인 담당이 아닌 결과를 조회하면 403을 던진다")
        void detail_serviceProvider_notOwner_forbidden() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            UUID serviceOfferingId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            UUID otherProviderId = UUID.randomUUID();

            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, requesterId, UserRole.SERVICE_PROVIDER);

            when(serviceResultQueryService.findDetailById(serviceResultId))
                    .thenReturn(Optional.of(detailResult(serviceResultId, serviceScheduleId, "정상 수행")));
            when(serviceScheduleQueryService.findById(serviceScheduleId))
                    .thenReturn(Optional.of(new ServiceScheduleResult(serviceScheduleId, UUID.randomUUID(), serviceOfferingId)));
            when(providerOfferingPort.findAssignedProviderId(serviceOfferingId)).thenReturn(otherProviderId);

            // when & then
            assertThatThrownBy(() -> serviceResultFacade.detail(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
        }

        @Test
        @DisplayName("수행 결과는 있으나 연결된 서비스 일정이 없으면 403을 던진다")
        void detail_scheduleNotFound_forbidden() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, UUID.randomUUID(), UserRole.PATIENT);

            when(serviceResultQueryService.findDetailById(serviceResultId))
                    .thenReturn(Optional.of(detailResult(serviceResultId, serviceScheduleId, null)));
            when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> serviceResultFacade.detail(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(carePlanPort, never()).findCarePlanRange(any());
            verify(providerOfferingPort, never()).findAssignedProviderId(any());
        }

        @Test
        @DisplayName("퇴원 예정자/서비스 제공자가 아닌 역할이면 403을 던진다")
        void detail_otherRole_forbidden() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            ServiceResultDetailQuery query = new ServiceResultDetailQuery(serviceResultId, UUID.randomUUID(), UserRole.ADMIN);

            when(serviceResultQueryService.findDetailById(serviceResultId))
                    .thenReturn(Optional.of(detailResult(serviceResultId, serviceScheduleId, null)));
            when(serviceScheduleQueryService.findById(serviceScheduleId))
                    .thenReturn(Optional.of(new ServiceScheduleResult(serviceScheduleId, UUID.randomUUID(), UUID.randomUUID())));

            // when & then
            assertThatThrownBy(() -> serviceResultFacade.detail(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

            verify(carePlanPort, never()).findCarePlanRange(any());
            verify(providerOfferingPort, never()).findAssignedProviderId(any());
        }
    }
}
