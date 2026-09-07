package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.common.PageableFactory;
import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.response.PageResponse;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;
import com.todak_todag.provider_service.provider.application.service.query.ProvideServiceQueryService;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/provide-services")
@RequiredArgsConstructor
public class ProvideServiceApiController implements ProvideServiceApiSpec {

    private final ProvideServiceQueryService provideServiceQueryService;

    @Override
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