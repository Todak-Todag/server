package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.common.PageableFactory;
import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.response.PageResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.application.facade.ServiceOfferingFacade;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingRegionSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingRegionSearchResult;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingRegionSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/service-offerings")
@RequiredArgsConstructor
public class ServiceOfferingAdminApiController {

    private final ServiceOfferingFacade serviceOfferingFacade;

    @GetMapping("/regions/{regionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER')")
    public ApiResponse<PageResponse<ServiceOfferingRegionSearchResponse>> searchByRegion(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("regionId") UUID regionId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Page<ServiceOfferingRegionSearchResult> results = serviceOfferingFacade.searchByRegion(
                new ServiceOfferingRegionSearchQuery(
                        regionId,
                        user.getUserId(),
                        user.getRole(),
                        PageableFactory.of(page, size, sort)
                )
        );

        return ApiResponse.ok(
                "지역별 제공 서비스 목록 조회 성공",
                PageResponse.of(results, ServiceOfferingRegionSearchResponse::from)
        );
    }
}
