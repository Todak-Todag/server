package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkCreateCommand;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideWorkCommandService;
import com.todak_todag.provider_service.provider.presentation.request.ProvideWorkCreateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ProvideWorkCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-offerings/{serviceOfferingId}/provide-works")
@RequiredArgsConstructor
public class ProvideWorkApiController {

    private final ProvideWorkCommandService provideWorkCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ApiResponse<ProvideWorkCreateResponse> create(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("serviceOfferingId") UUID serviceOfferingId,
            @Valid @RequestBody ProvideWorkCreateRequest request
    ) {
        ProvideWorkCreateResult result = provideWorkCommandService.create(
                new ProvideWorkCreateCommand(
                        serviceOfferingId,
                        user.getUserId(),
                        request.day(),
                        request.startedAt(),
                        request.finishedAt()
                )
        );

        return ApiResponse.created("제공 가능 일정 등록 성공", ProvideWorkCreateResponse.from(result));
    }
}