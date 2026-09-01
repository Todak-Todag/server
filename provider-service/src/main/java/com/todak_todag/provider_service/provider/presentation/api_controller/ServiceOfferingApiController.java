package com.todak_todag.provider_service.provider.presentation.api_controller;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCommand;
import com.todak_todag.provider_service.provider.application.command_service.ServiceOfferingCommandService;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingResult;
import com.todak_todag.provider_service.provider.presentation.request.ServiceOfferingRequest;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/service-offerings")
@RequiredArgsConstructor
public class ServiceOfferingApiController {

    private final ServiceOfferingCommandService serviceOfferingCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ApiResponse<ServiceOfferingResponse.Create> create(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody ServiceOfferingRequest.Create request
    ) {
        ServiceOfferingResult.Create result = serviceOfferingCommandService.create(
                new ServiceOfferingCommand.Create(user.getUserId(), request.provideServiceId())
        );

        return ApiResponse.created("제공 서비스 등록 성공", ServiceOfferingResponse.Create.from(result));
    }
}