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

    // 삭제 이전에 잡힌 일정의 담당 제공자는 삭제 뒤에도 바뀌지 않으므로 삭제 이력까지 조회한다
    public ServiceOfferingProviderResult findProvider(UUID serviceOfferingId) {
        UUID providerId = serviceOfferingQueryRepository.findProviderIdIncludingDeleted(serviceOfferingId)
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        return new ServiceOfferingProviderResult(providerId);
    }

    // 삭제 이전 일정·결과가 제공자 목록에서 빠지지 않도록 삭제 이력까지 포함한다
    public ServiceOfferingIdsResult findIdsByProvider(UUID providerId) {
        return new ServiceOfferingIdsResult(
                serviceOfferingQueryRepository.findIdsByProviderIdIncludingDeleted(providerId));
    }

    private UUID resolveTargetProviderId(ServiceOfferingSearchQuery query) {
        if (query.userRole() == UserRole.ADMIN) {
            // ADMIN은 담당 지역 내 제공자만 조회 가능
            // ADMIN의 담당 지역 검증은 Facade에서 마친 뒤 들어온다
            return query.providerId() != null ? query.providerId() : query.userId();
        }

        if (query.providerId() != null && !query.providerId().equals(query.userId())) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        return query.userId();
    }
}
