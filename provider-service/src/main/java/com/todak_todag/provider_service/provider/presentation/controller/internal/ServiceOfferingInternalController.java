package com.todak_todag.provider_service.provider.presentation.controller.internal;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingIdsResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingProviderResult;
import com.todak_todag.provider_service.provider.application.service.query.ServiceOfferingQueryService;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingIdsResponse;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingProviderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/service-offerings")
@RequiredArgsConstructor
public class ServiceOfferingInternalController {

    private final ServiceOfferingQueryService serviceOfferingQueryService;

    @GetMapping("/{serviceOfferingId}")
    public ApiResponse<ServiceOfferingProviderResponse> findProvider(
            @PathVariable("serviceOfferingId") UUID serviceOfferingId
    ) {
        ServiceOfferingProviderResult result =
                serviceOfferingQueryService.findProvider(serviceOfferingId);

        return ApiResponse.ok("제공 서비스 제공자 조회 성공", ServiceOfferingProviderResponse.from(result));
    }

    @GetMapping
    public ApiResponse<ServiceOfferingIdsResponse> findIdsByProvider(
            @RequestParam("providerId") UUID providerId
    ) {
        ServiceOfferingIdsResult result =
                serviceOfferingQueryService.findIdsByProvider(providerId);

        return ApiResponse.ok("제공 서비스 목록 조회 성공", ServiceOfferingIdsResponse.from(result));
    }
}