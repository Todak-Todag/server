package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.common.PageableFactory;
import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.response.PageResponse;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceCreateResult;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideServiceCommandService;
import com.todak_todag.provider_service.provider.application.service.query.ProvideServiceQueryService;
import com.todak_todag.provider_service.provider.presentation.request.ProvideServiceCreateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceCreateResponse;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/provide-services")
@RequiredArgsConstructor
public class ProvideServiceApiController {

    private final ProvideServiceCommandService provideServiceCommandService;
    private final ProvideServiceQueryService provideServiceQueryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<ProvideServiceCreateResponse> create(
            @Valid @RequestBody ProvideServiceCreateRequest request
    ) {
        ProvideServiceCreateResult result = provideServiceCommandService.create(request.toCommand());

        return ApiResponse.created("서비스 종류 등록 성공", ProvideServiceCreateResponse.from(result));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProvideServiceSearchResponse>> search(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Page<ProvideServiceSearchResult> results =
                provideServiceQueryService.search(PageableFactory.of(page, size, null));

        return ApiResponse.ok(
                "서비스 종류 목록 조회 성공",
                PageResponse.of(results, ProvideServiceSearchResponse::from)
        );
    }
}