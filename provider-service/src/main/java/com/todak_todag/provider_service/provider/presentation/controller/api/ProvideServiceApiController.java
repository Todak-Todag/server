package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceCreateResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideServiceCommandService;
import com.todak_todag.provider_service.provider.presentation.request.ProvideServiceCreateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provide-services")
@RequiredArgsConstructor
public class ProvideServiceApiController {

    private final ProvideServiceCommandService provideServiceCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<ProvideServiceCreateResponse> create(
            @Valid @RequestBody ProvideServiceCreateRequest request
    ) {
        ProvideServiceCreateResult result = provideServiceCommandService.create(request.toCommand());

        return ApiResponse.created("서비스 종류 등록 성공", ProvideServiceCreateResponse.from(result));
    }
}