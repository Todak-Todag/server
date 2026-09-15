package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.application.port.UserPort;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingRegionSearchQuery;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingRegionSearchResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;
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
    private UserPort userPort;

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
            given(offering.isOwnedBy(providerId)).willReturn(true);
            return offering;
        }

        @Test
        @DisplayName("확정 일정 여부와 관계없이 CommandService에 위임한다")
        void delete_success() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingQueryRepository.findById(serviceOfferingId))
                    .willReturn(Optional.of(offering));

            serviceOfferingFacade.delete(command());

            verify(serviceOfferingCommandService).delete(any(ServiceOfferingDeleteCommand.class));
        }

        @Test
        @DisplayName("존재하지 않으면 SERVICE_OFFERING_NOT_FOUND")
        void delete_notFound() {
            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> serviceOfferingFacade.delete(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);

            verify(serviceOfferingCommandService, never()).delete(any());
        }

        @Test
        @DisplayName("본인 소유가 아니면 AUTH_FORBIDDEN")
        void delete_notOwner() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.isOwnedBy(providerId)).willReturn(false);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));

            assertThatThrownBy(() -> serviceOfferingFacade.delete(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(serviceOfferingCommandService, never()).delete(any());
        }

        private ServiceOfferingDeleteCommand adminCommand() {
            return new ServiceOfferingDeleteCommand(serviceOfferingId, adminId, UserRole.ADMIN);
        }

        @Test
        @DisplayName("ADMIN은 담당 지역이면 본인 소유가 아니어도 CommandService에 위임한다")
        void delete_admin_sameRegion() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.getRegionId()).willReturn(regionId);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(userPort.findRegionIdByUserId(adminId)).willReturn(regionId);

            serviceOfferingFacade.delete(adminCommand());

            verify(serviceOfferingCommandService).delete(any(ServiceOfferingDeleteCommand.class));
            verify(offering, never()).isOwnedBy(any());
        }

        @Test
        @DisplayName("ADMIN이라도 담당 지역이 아니면 AUTH_FORBIDDEN")
        void delete_admin_otherRegion() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.getRegionId()).willReturn(otherRegionId);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(userPort.findRegionIdByUserId(adminId)).willReturn(regionId);

            assertThatThrownBy(() -> serviceOfferingFacade.delete(adminCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(serviceOfferingCommandService, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("제공자별 조회")
    class Search {

        private ServiceOfferingSearchQuery query(UUID targetProviderId, UUID requesterId, UserRole role) {
            return new ServiceOfferingSearchQuery(targetProviderId, requesterId, role, pageable);
        }

        private Page<ServiceOfferingSearchResult> page() {
            return new PageImpl<>(
                    List.of(new ServiceOfferingSearchResult(serviceOfferingId, provideServiceId, "방문간호", Instant.now())),
                    pageable,
                    1
            );
        }

        @Test
        @DisplayName("ADMIN이 담당 지역 제공자를 지정하면 QueryService에 위임한다")
        void search_admin_sameRegion() {
            given(userPort.findRegionIdByUserId(providerId)).willReturn(regionId);
            given(userPort.findRegionIdByUserId(adminId)).willReturn(regionId);
            given(serviceOfferingQueryService.search(any(ServiceOfferingSearchQuery.class))).willReturn(page());

            Page<ServiceOfferingSearchResult> results =
                    serviceOfferingFacade.search(query(providerId, adminId, UserRole.ADMIN));

            assertThat(results.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("ADMIN이 다른 지역 제공자를 지정하면 조회하지 않고 AUTH_FORBIDDEN")
        void search_admin_otherRegion() {
            given(userPort.findRegionIdByUserId(providerId)).willReturn(otherRegionId);
            given(userPort.findRegionIdByUserId(adminId)).willReturn(regionId);

            assertThatThrownBy(() -> serviceOfferingFacade.search(query(providerId, adminId, UserRole.ADMIN)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(serviceOfferingQueryService, never()).search(any());
        }

        @Test
        @DisplayName("ADMIN이 제공자를 지정하지 않으면 지역 검증 없이 위임한다")
        void search_admin_withoutProvider() {
            given(serviceOfferingQueryService.search(any(ServiceOfferingSearchQuery.class))).willReturn(page());

            serviceOfferingFacade.search(query(null, adminId, UserRole.ADMIN));

            verify(userPort, never()).findRegionIdByUserId(any());
        }

        @Test
        @DisplayName("제공자 본인 조회는 지역 검증 없이 위임한다")
        void search_provider_noRegionCheck() {
            given(serviceOfferingQueryService.search(any(ServiceOfferingSearchQuery.class))).willReturn(page());

            serviceOfferingFacade.search(query(null, providerId, UserRole.SERVICE_PROVIDER));

            verify(userPort, never()).findRegionIdByUserId(any());
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
}
