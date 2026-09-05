package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultSearchQuery;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("존재하지 않는 일정이면 404를 던지고 provider 조회를 하지 않는다 (07번 문서는 404를 명시적으로 요구)")
    void register_notFound_notFoundWithoutProviderLookup() {
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
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND);

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
        @DisplayName("퇴원 예정자가 담당하는 servicePreferenceId가 하나도 없으면 DB 조회 없이 빈 페이지를 반환한다 (01번에서 확정된 처리 방식)")
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
        @DisplayName("서비스 제공자가 담당하는 serviceOfferingId가 하나도 없으면 DB 조회 없이 빈 페이지를 반환한다 (01번에서 확정된 처리 방식)")
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
        @DisplayName("퇴원 예정자/서비스 제공자가 아닌 역할이면 403을 던진다 (방어 코드)")
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
}
