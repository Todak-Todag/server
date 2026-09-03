package com.todak_todag.schedule_service.schedule.infrastructure.client.provider;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.ProviderServiceOfferingInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.ServiceOfferingIdListInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

// schedule-service -> provider-service Internal API
// 인증 헤더(X-Internal-Api-Key)는 요청마다 직접 붙이지 않고 FeignConfig의 RequestInterceptor가 전담
@FeignClient(name = "provider-service")
public interface ProviderServiceOfferingClient {

    // serviceOfferingId를 담당하는 providerId 조회
    @GetMapping("/internal/v1/service-offerings/{serviceOfferingId}")
    ApiResponse<ProviderServiceOfferingInternalResponse> findServiceOffering(
            @PathVariable UUID serviceOfferingId
    );

    // providerId가 담당하는 모든 serviceOfferingId 목록 조회
    @GetMapping("/internal/v1/service-offerings")
    ApiResponse<ServiceOfferingIdListInternalResponse> findServiceOfferingIds(
            @RequestParam UUID providerId
    );
}
