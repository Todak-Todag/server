package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("제공 서비스 목록 조회")
class ServiceOfferingQueryServiceTest {

    private final UUID providerId = UUID.randomUUID();
    private final UUID otherProviderId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    private final Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Mock
    private ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    @InjectMocks
    private ServiceOfferingQueryService serviceOfferingQueryService;

    private ServiceOfferingSearchQuery query(UUID requestedProviderId, UUID userId, UserRole userRole) {
        return new ServiceOfferingSearchQuery(requestedProviderId, userId, userRole, pageable);
    }

    private Page<ServiceOfferingSearchResult> page() {
        return new PageImpl<>(
                List.of(new ServiceOfferingSearchResult(
                        UUID.randomUUID(), UUID.randomUUID(), "방문간호", Instant.now())),
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
        @DisplayName("본인의 제공 서비스 목록을 조회한다")
        void search_ownList() {
            given(serviceOfferingQueryRepository.searchByProviderId(providerId, pageable))
                    .willReturn(page());

            Page<ServiceOfferingSearchResult> results = serviceOfferingQueryService.search(
                    query(null, providerId, UserRole.SERVICE_PROVIDER));

            assertThat(results.getContent()).hasSize(1);
            assertThat(capturedProviderId()).isEqualTo(providerId);
        }

        @Test
        @DisplayName("타인의 providerId를 넘겨도 본인 기준으로 조회한다")
        void search_ignoresRequestedProviderId() {
            given(serviceOfferingQueryRepository.searchByProviderId(providerId, pageable))
                    .willReturn(page());

            serviceOfferingQueryService.search(
                    query(otherProviderId, providerId, UserRole.SERVICE_PROVIDER));

            assertThat(capturedProviderId()).isEqualTo(providerId);
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
}