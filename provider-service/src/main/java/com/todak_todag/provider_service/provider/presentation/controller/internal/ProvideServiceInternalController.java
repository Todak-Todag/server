package com.todak_todag.provider_service.provider.presentation.controller.internal;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.CommonErrorCode;
import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceInfoResult;
import com.todak_todag.provider_service.provider.application.service.query.ProvideServiceQueryService;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceInfoListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/provide-services")
@RequiredArgsConstructor
public class ProvideServiceInternalController {

    private final ProvideServiceQueryService provideServiceQueryService;

    @GetMapping
    public ApiResponse<ProvideServiceInfoListResponse> findAllByIds(
            @RequestParam("provideServiceIds") List<UUID> provideServiceIds
    ) {
        // 파라미터 누락은 MissingServletRequestParameterException으로 잡히지만
        // ?provideServiceIds= 처럼 값이 비면 빈 리스트로 바인딩되므로 직접 막는다
        if (provideServiceIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        List<ProvideServiceInfoResult> results =
                provideServiceQueryService.findAllByIds(provideServiceIds);

        return ApiResponse.ok("서비스 종류 목록 조회 성공", ProvideServiceInfoListResponse.from(results));
    }
}