package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkUpdateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.application.port.UserPort;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingRegionSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkUpdateResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingRegionSearchResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideWorkCommandService;
import com.todak_todag.provider_service.provider.application.service.command.ServiceOfferingCommandService;
import com.todak_todag.provider_service.provider.application.service.query.ServiceOfferingQueryService;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("제공 서비스 Facade")
class ServiceOfferingFacadeTest {

    private final UUID providerId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID serviceOfferingId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID otherRegionId = UUID.randomUUID();

    private final Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Mock
    private ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    @Mock
    private ProvideServiceQueryRepository provideServiceQueryRepository;

    @Mock
    private ServiceOfferingCommandService serviceOfferingCommandService;

    @Mock
    private ServiceOfferingQueryService serviceOfferingQueryService;

    @Mock
    private ProvideWorkCommandService provideWorkCommandService;

    @Mock
    private UserPort userPort;

    @Mock
    private SchedulePort schedulePort;

    @InjectMocks
    private ServiceOfferingFacade serviceOfferingFacade;

    @Nested
    @DisplayName("등록")
    class Create {

        private ServiceOfferingCreateCommand command() {
            return new ServiceOfferingCreateCommand(providerId, provideServiceId);
        }

        @Test
        @DisplayName("User-Service에서 조회한 regionId를 CommandService에 전달한다")
        void create_success() {
            ServiceOfferingCreateResult expected =
                    new ServiceOfferingCreateResult(serviceOfferingId, providerId, Instant.now());

            given(provideServiceQueryRepository.existsById(provideServiceId)).willReturn(true);
            given(serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId))
                    .willReturn(false);
            given(userPort.findRegionIdByUserId(providerId)).willReturn(regionId);
            given(serviceOfferingCommandService.create(any(ServiceOfferingCreateCommand.class), eq(regionId)))
                    .willReturn(expected);

            ServiceOfferingCreateResult result = serviceOfferingFacade.create(command());

            assertThat(result).isEqualTo(expected);
            verify(serviceOfferingCommandService).create(any(ServiceOfferingCreateCommand.class), eq(regionId));
        }

        @Test
        @DisplayName("존재하지 않는 서비스 종류면 User-Service를 호출하지 않는다")
        void create_provideServiceNotFound_noExternalCall() {
            given(provideServiceQueryRepository.existsById(provideServiceId)).willReturn(false);

            assertThatThrownBy(() -> serviceOfferingFacade.create(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_SERVICE_NOT_FOUND);

            verify(userPort, never()).findRegionIdByUserId(any());
            verify(serviceOfferingCommandService, never()).create(any(), any());
        }

        @Test
        @DisplayName("이미 등록한 서비스 종류면 User-Service를 호출하지 않는다")
        void create_duplicate_noExternalCall() {
            given(provideServiceQueryRepository.existsById(provideServiceId)).willReturn(true);
            given(serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId))
                    .willReturn(true);

            assertThatThrownBy(() -> serviceOfferingFacade.create(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_DUPLICATE);

            verify(userPort, never()).findRegionIdByUserId(any());
            verify(serviceOfferingCommandService, never()).create(any(), any());
        }

        @Test
        @DisplayName("담당 지역이 없으면 PROVIDER_REGION_NOT_ASSIGNED")
        void create_regionNotAssigned() {
            given(provideServiceQueryRepository.existsById(provideServiceId)).willReturn(true);
            given(serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId))
                    .willReturn(false);
            given(userPort.findRegionIdByUserId(providerId)).willReturn(null);

            assertThatThrownBy(() -> serviceOfferingFacade.create(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDER_REGION_NOT_ASSIGNED);

            verify(serviceOfferingCommandService, never()).create(any(), any());
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        private ServiceOfferingDeleteCommand command() {
            return new ServiceOfferingDeleteCommand(serviceOfferingId, providerId, UserRole.SERVICE_PROVIDER);
        }

        private ServiceOffering ownedOffering() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.getId()).willReturn(serviceOfferingId);
            given(offering.isOwnedBy(providerId)).willReturn(true);
            return offering;
        }

        @Test
        @DisplayName("확정 일정이 없으면 CommandService에 위임한다")
        void delete_success() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingQueryRepository.findById(serviceOfferingId))
                    .willReturn(Optional.of(offering));
            given(schedulePort.existsConfirmedSchedule(serviceOfferingId)).willReturn(false);

            serviceOfferingFacade.delete(command());

            verify(serviceOfferingCommandService).delete(any(ServiceOfferingDeleteCommand.class));
        }

        @Test
        @DisplayName("존재하지 않으면 Schedule-Service를 호출하지 않는다")
        void delete_notFound_noExternalCall() {
            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> serviceOfferingFacade.delete(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);

            verify(schedulePort, never()).existsConfirmedSchedule(any());
            verify(serviceOfferingCommandService, never()).delete(any());
        }

        @Test
        @DisplayName("본인 소유가 아니면 Schedule-Service를 호출하지 않는다")
        void delete_notOwner_noExternalCall() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.isOwnedBy(providerId)).willReturn(false);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));

            assertThatThrownBy(() -> serviceOfferingFacade.delete(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(schedulePort, never()).existsConfirmedSchedule(any());
            verify(serviceOfferingCommandService, never()).delete(any());
        }

        @Test
        @DisplayName("확정된 일정이 있으면 SERVICE_OFFERING_SCHEDULE_EXISTS")
        void delete_scheduleExists() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingQueryRepository.findById(serviceOfferingId))
                    .willReturn(Optional.of(offering));
            given(schedulePort.existsConfirmedSchedule(serviceOfferingId)).willReturn(true);

            assertThatThrownBy(() -> serviceOfferingFacade.delete(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_SCHEDULE_EXISTS);

            verify(serviceOfferingCommandService, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("지역별 조회")
    class SearchByRegion {

        private ServiceOfferingRegionSearchQuery query(UserRole userRole) {
            return new ServiceOfferingRegionSearchQuery(regionId, adminId, userRole, pageable);
        }

        private Page<ServiceOfferingRegionSearchResult> page() {
            return new PageImpl<>(
                    List.of(new ServiceOfferingRegionSearchResult(
                            serviceOfferingId, providerId, provideServiceId, "방문간호")),
                    pageable,
                    1
            );
        }

        @Test
        @DisplayName("담당 지역이면 QueryService에 위임한다")
        void searchByRegion_admin_success() {
            given(userPort.findRegionIdByUserId(adminId)).willReturn(regionId);
            given(serviceOfferingQueryService.searchByRegion(any(ServiceOfferingRegionSearchQuery.class)))
                    .willReturn(page());

            Page<ServiceOfferingRegionSearchResult> results =
                    serviceOfferingFacade.searchByRegion(query(UserRole.ADMIN));

            assertThat(results.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("MASTER는 User-Service를 호출하지 않는다")
        void searchByRegion_master_noExternalCall() {
            given(serviceOfferingQueryService.searchByRegion(any(ServiceOfferingRegionSearchQuery.class)))
                    .willReturn(page());

            Page<ServiceOfferingRegionSearchResult> results =
                    serviceOfferingFacade.searchByRegion(query(UserRole.MASTER));

            assertThat(results.getContent()).hasSize(1);
            verify(userPort, never()).findRegionIdByUserId(any());
        }

        @Test
        @DisplayName("담당 지역이 아니면 조회하지 않고 AUTH_FORBIDDEN")
        void searchByRegion_otherRegion() {
            given(userPort.findRegionIdByUserId(adminId)).willReturn(otherRegionId);

            assertThatThrownBy(() -> serviceOfferingFacade.searchByRegion(query(UserRole.ADMIN)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(serviceOfferingQueryService, never()).searchByRegion(any());
        }

        @Test
        @DisplayName("담당 지역이 지정되지 않은 운영자면 조회하지 않고 AUTH_FORBIDDEN")
        void searchByRegion_nullRegion() {
            given(userPort.findRegionIdByUserId(adminId)).willReturn(null);

            assertThatThrownBy(() -> serviceOfferingFacade.searchByRegion(query(UserRole.ADMIN)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(serviceOfferingQueryService, never()).searchByRegion(any());
        }
    }

    @Nested
    @DisplayName("제공 가능 일정 수정")
    class UpdateProvideWork {

        private final UUID provideWorkId = UUID.randomUUID();

        private ProvideWorkUpdateCommand command() {
            return new ProvideWorkUpdateCommand(
                    serviceOfferingId, provideWorkId, providerId, 1, LocalTime.of(9, 0), LocalTime.of(13, 0));
        }

        private ServiceOffering ownedOffering() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.getId()).willReturn(serviceOfferingId);
            given(offering.isOwnedBy(providerId)).willReturn(true);
            return offering;
        }

        @Test
        @DisplayName("확정 일정이 없으면 CommandService에 위임한다")
        void updateProvideWork_success() {
            ServiceOffering offering = ownedOffering();
            ProvideWorkUpdateResult expected = new ProvideWorkUpdateResult(provideWorkId);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(schedulePort.existsConfirmedSchedule(serviceOfferingId)).willReturn(false);
            given(provideWorkCommandService.update(any(ProvideWorkUpdateCommand.class))).willReturn(expected);

            ProvideWorkUpdateResult result = serviceOfferingFacade.updateProvideWork(command());

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않으면 Schedule-Service를 호출하지 않는다")
        void updateProvideWork_notFound_noExternalCall() {
            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> serviceOfferingFacade.updateProvideWork(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);

            verify(schedulePort, never()).existsConfirmedSchedule(any());
            verify(provideWorkCommandService, never()).update(any());
        }

        @Test
        @DisplayName("본인 소유가 아니면 Schedule-Service를 호출하지 않는다")
        void updateProvideWork_notOwner_noExternalCall() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.isOwnedBy(providerId)).willReturn(false);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));

            assertThatThrownBy(() -> serviceOfferingFacade.updateProvideWork(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(schedulePort, never()).existsConfirmedSchedule(any());
            verify(provideWorkCommandService, never()).update(any());
        }

        @Test
        @DisplayName("확정된 일정이 있으면 CommandService를 호출하지 않는다")
        void updateProvideWork_scheduleExists() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(schedulePort.existsConfirmedSchedule(serviceOfferingId)).willReturn(true);

            assertThatThrownBy(() -> serviceOfferingFacade.updateProvideWork(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_SCHEDULE_EXISTS);

            verify(provideWorkCommandService, never()).update(any());
        }
    }
}
