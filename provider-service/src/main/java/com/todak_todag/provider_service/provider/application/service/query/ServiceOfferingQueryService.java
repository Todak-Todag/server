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
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceOfferingQueryService {

    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    public Page<ServiceOfferingSearchResult> search(ServiceOfferingSearchQuery query) {
        return serviceOfferingQueryRepository
                .searchByProviderId(resolveTargetProviderId(query), query.pageable())
                .map(view -> new ServiceOfferingSearchResult(
                        view.serviceOfferingId(),
                        view.provideServiceId(),
                        view.provideServiceName(),
                        view.createdAt()
                ));
    }

    // 담당 지역 검증은 Facade가 트랜잭션 밖에서 수행한다
    public Page<ServiceOfferingRegionSearchResult> searchByRegion(ServiceOfferingRegionSearchQuery query) {
        return serviceOfferingQueryRepository
                .searchByRegionId(query.regionId(), query.pageable())
                .map(view -> new ServiceOfferingRegionSearchResult(
                        view.serviceOfferingId(),
                        view.providerId(),
                        view.provideServiceId(),
                        view.provideServiceName()
                ));
    }

    public ServiceOfferingProviderResult findProvider(UUID serviceOfferingId) {
        ServiceOffering serviceOffering = serviceOfferingQueryRepository.findById(serviceOfferingId)
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        return new ServiceOfferingProviderResult(serviceOffering.getProviderId());
    }

    public ServiceOfferingIdsResult findIdsByProvider(UUID providerId) {
        return new ServiceOfferingIdsResult(
                serviceOfferingQueryRepository.findIdsByProviderId(providerId));
    }

    private UUID resolveTargetProviderId(ServiceOfferingSearchQuery query) {
        if (query.userRole() == UserRole.ADMIN) {
            // ADMIN은 담당 지역 내 제공자만 조회 가능
            // TODO: User-Service 사용자 조회 API 구현 후 요청자·대상 지역 일치 검증 추가
            //       그 전까지는 ADMIN이면 모든 지역을 조회할 수 있다
            return query.providerId() != null ? query.providerId() : query.userId();
        }

        if (query.providerId() != null && !query.providerId().equals(query.userId())) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        return query.userId();
    }
}
