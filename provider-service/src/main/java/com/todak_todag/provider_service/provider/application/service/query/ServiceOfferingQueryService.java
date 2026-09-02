package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;
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
        return serviceOfferingQueryRepository.searchByProviderId(
                resolveTargetProviderId(query),
                query.pageable()
        );
    }

    private UUID resolveTargetProviderId(ServiceOfferingSearchQuery query) {
        if (query.userRole() == UserRole.ADMIN) {
            // TODO: User-Service 사용자 조회 API 구현 후 ADMIN 담당 지역 검증 추가
            return query.providerId() != null ? query.providerId() : query.userId();
        }

        if (query.providerId() != null && !query.providerId().equals(query.userId())) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        return query.userId();
    }
}