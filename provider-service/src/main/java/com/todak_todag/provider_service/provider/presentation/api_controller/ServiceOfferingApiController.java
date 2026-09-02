package com.todak_todag.provider_service.provider.presentation.api_controller;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.application.command_service.ServiceOfferingCommandService;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.presentation.request.ServiceOfferingCreateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-offerings")
@RequiredArgsConstructor
public class ServiceOfferingApiController {

    private final ServiceOfferingCommandService serviceOfferingCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ApiResponse<ServiceOfferingCreateResponse> create(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody ServiceOfferingCreateRequest request
    ) {
        ServiceOfferingCreateResult result = serviceOfferingCommandService.create(
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
        serviceOfferingCommandService.delete(
                new ServiceOfferingDeleteCommand(serviceOfferingId, user.getUserId(), user.getRole())
        );
    }
}