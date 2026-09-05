package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingRegionSearchQuery;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingIdsResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingProviderResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingRegionSearchResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingView;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("제공 서비스 목록 조회")
class ServiceOfferingQueryServiceTest {

    private final UUID providerId = UUID.randomUUID();
    private final UUID otherProviderId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID serviceOfferingId = UUID.randomUUID();

    private final Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Mock
    private ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    @InjectMocks
    private ServiceOfferingQueryService serviceOfferingQueryService;

    private ServiceOfferingSearchQuery query(UUID requestedProviderId, UUID userId, UserRole userRole) {
        return new ServiceOfferingSearchQuery(requestedProviderId, userId, userRole, pageable);
    }

    private Page<ServiceOfferingView> page() {
        return new PageImpl<>(
                List.of(new ServiceOfferingView(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "방문간호", Instant.now())),
                pageable,
                1
        );
    }

    private UUID capturedProviderId() {
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(serviceOfferingQueryRepository).searchByProviderId(captor.capture(), any());
        return captor.getValue();
    }

    @Nested
    @DisplayName("SERVICE_PROVIDER")
    class ServiceProvider {

        @Test
        @DisplayName("providerId가 없으면 본인 목록을 조회한다")
        void search_ownList() {
            given(serviceOfferingQueryRepository.searchByProviderId(providerId, pageable))
                    .willReturn(page());

            Page<ServiceOfferingSearchResult> results = serviceOfferingQueryService.search(
                    query(null, providerId, UserRole.SERVICE_PROVIDER));

            assertThat(results.getContent()).hasSize(1);
            assertThat(capturedProviderId()).isEqualTo(providerId);
        }

        @Test
        @DisplayName("본인의 providerId를 넘기면 정상 조회된다")
        void search_ownProviderId() {
            given(serviceOfferingQueryRepository.searchByProviderId(providerId, pageable))
                    .willReturn(page());

            serviceOfferingQueryService.search(
                    query(providerId, providerId, UserRole.SERVICE_PROVIDER));

            assertThat(capturedProviderId()).isEqualTo(providerId);
        }

        @Test
        @DisplayName("타인의 providerId를 넘기면 AUTH_FORBIDDEN")
        void search_otherProviderId() {
            assertThatThrownBy(() -> serviceOfferingQueryService.search(
                    query(otherProviderId, providerId, UserRole.SERVICE_PROVIDER)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(serviceOfferingQueryRepository, never()).searchByProviderId(any(), any());
        }
    }

    @Nested
    @DisplayName("ADMIN")
    class Admin {

        @Test
        @DisplayName("providerId로 특정 제공자의 목록을 조회한다")
        void search_byRequestedProviderId() {
            given(serviceOfferingQueryRepository.searchByProviderId(providerId, pageable))
                    .willReturn(page());

            serviceOfferingQueryService.search(
                    query(providerId, adminId, UserRole.ADMIN));

            assertThat(capturedProviderId()).isEqualTo(providerId);
        }

        @Test
        @DisplayName("providerId가 없으면 본인 기준으로 조회한다")
        void search_withoutProviderId() {
            given(serviceOfferingQueryRepository.searchByProviderId(adminId, pageable))
                    .willReturn(page());

            serviceOfferingQueryService.search(
                    query(null, adminId, UserRole.ADMIN));

            assertThat(capturedProviderId()).isEqualTo(adminId);
        }
    }

    @Nested
    @DisplayName("결과 없음")
    class Empty {

        @Test
        @DisplayName("등록한 제공 서비스가 없으면 빈 목록을 반환한다")
        void search_empty() {
            given(serviceOfferingQueryRepository.searchByProviderId(providerId, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<ServiceOfferingSearchResult> results = serviceOfferingQueryService.search(
                    query(null, providerId, UserRole.SERVICE_PROVIDER));

            assertThat(results.getContent()).isEmpty();
            assertThat(results.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("제공자 조회")
    class FindProvider {

        @Test
        @DisplayName("providerId를 반환한다")
        void findProvider_success() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.getProviderId()).willReturn(providerId);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId))
                    .willReturn(Optional.of(offering));

            ServiceOfferingProviderResult result =
                    serviceOfferingQueryService.findProvider(serviceOfferingId);

            assertThat(result.providerId()).isEqualTo(providerId);
        }

        @Test
        @DisplayName("존재하지 않으면 SERVICE_OFFERING_NOT_FOUND")
        void findProvider_notFound() {
            given(serviceOfferingQueryRepository.findById(serviceOfferingId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> serviceOfferingQueryService.findProvider(serviceOfferingId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("제공자별 제공 서비스 ID 목록 조회")
    class FindIdsByProvider {

        @Test
        @DisplayName("보유한 제공 서비스 ID 목록을 반환한다")
        void findIdsByProvider_success() {
            UUID providerId = UUID.randomUUID();
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();

            given(serviceOfferingQueryRepository.findIdsByProviderId(providerId))
                    .willReturn(List.of(first, second));

            ServiceOfferingIdsResult result =
                    serviceOfferingQueryService.findIdsByProvider(providerId);

            assertThat(result.serviceOfferingIds()).containsExactly(first, second);
        }

        @Test
        @DisplayName("보유한 제공 서비스가 없으면 빈 목록을 반환한다")
        void findIdsByProvider_empty() {
            UUID providerId = UUID.randomUUID();

            given(serviceOfferingQueryRepository.findIdsByProviderId(providerId))
                    .willReturn(List.of());

            ServiceOfferingIdsResult result =
                    serviceOfferingQueryService.findIdsByProvider(providerId);

            assertThat(result.serviceOfferingIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("지역별 조회")
    class RegionSearch {

        private ServiceOfferingRegionSearchQuery query(UUID requestedRegionId, UserRole userRole) {
            return new ServiceOfferingRegionSearchQuery(requestedRegionId, adminId, userRole, pageable);
        }

        @Test
        @DisplayName("요청한 지역의 제공 서비스 목록을 조회한다")
        void searchByRegion_success() {
            given(serviceOfferingQueryRepository.searchByRegionId(regionId, pageable))
                    .willReturn(page());

            Page<ServiceOfferingRegionSearchResult> results =
                    serviceOfferingQueryService.searchByRegion(query(regionId, UserRole.ADMIN));

            assertThat(results.getContent()).hasSize(1);
            verify(serviceOfferingQueryRepository).searchByRegionId(regionId, pageable);
        }

        @Test
        @DisplayName("View의 providerId가 결과에 그대로 매핑된다")
        void searchByRegion_mapsProviderId() {
            UUID viewServiceOfferingId = UUID.randomUUID();
            UUID viewProviderId = UUID.randomUUID();
            UUID viewProvideServiceId = UUID.randomUUID();

            given(serviceOfferingQueryRepository.searchByRegionId(regionId, pageable))
                    .willReturn(new PageImpl<>(
                            List.of(new ServiceOfferingView(
                                    viewServiceOfferingId,
                                    viewProviderId,
                                    viewProvideServiceId,
                                    "방문간호",
                                    Instant.now())),
                            pageable,
                            1
                    ));

            ServiceOfferingRegionSearchResult result =
                    serviceOfferingQueryService.searchByRegion(query(regionId, UserRole.ADMIN))
                            .getContent()
                            .getFirst();

            assertThat(result.serviceOfferingId()).isEqualTo(viewServiceOfferingId);
            assertThat(result.providerId()).isEqualTo(viewProviderId);
            assertThat(result.provideServiceId()).isEqualTo(viewProvideServiceId);
            assertThat(result.provideServiceName()).isEqualTo("방문간호");
        }
    }
}
