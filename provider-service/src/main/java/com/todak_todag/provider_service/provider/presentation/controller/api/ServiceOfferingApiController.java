package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.common.PageableFactory;
import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.response.PageResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.application.facade.ServiceOfferingFacade;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;
import com.todak_todag.provider_service.provider.application.service.query.ServiceOfferingQueryService;
import com.todak_todag.provider_service.provider.presentation.request.ServiceOfferingCreateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingCreateResponse;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-offerings")
@RequiredArgsConstructor
public class ServiceOfferingApiController {

    private final ServiceOfferingFacade serviceOfferingFacade;
    private final ServiceOfferingQueryService serviceOfferingQueryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ApiResponse<ServiceOfferingCreateResponse> create(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody ServiceOfferingCreateRequest request
    ) {
        ServiceOfferingCreateResult result = serviceOfferingFacade.create(
                new ServiceOfferingCreateCommand(user.getUserId(), request.provideServiceId())
        );

        return ApiResponse.created("제공 서비스 등록 성공", ServiceOfferingCreateResponse.from(result));
    }

    @DeleteMapping("/{serviceOfferingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SERVICE_PROVIDER', 'ADMIN')")
    public void delete(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("serviceOfferingId") UUID serviceOfferingId
    ) {
        serviceOfferingFacade.delete(
                new ServiceOfferingDeleteCommand(serviceOfferingId, user.getUserId(), user.getRole())
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_PROVIDER', 'ADMIN')")
    public ApiResponse<PageResponse<ServiceOfferingSearchResponse>> search(
            @AuthenticationPrincipal UserContext user,
            @RequestParam(value = "providerId", required = false) UUID providerId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Page<ServiceOfferingSearchResult> results = serviceOfferingQueryService.search(
                new ServiceOfferingSearchQuery(
                        providerId,
                        user.getUserId(),
                        user.getRole(),
                        PageableFactory.of(page, size, sort)
                )
        );

        return ApiResponse.ok(
                "제공 서비스 목록 조회 성공",
                PageResponse.of(results, ServiceOfferingSearchResponse::from)
        );
    }
}
