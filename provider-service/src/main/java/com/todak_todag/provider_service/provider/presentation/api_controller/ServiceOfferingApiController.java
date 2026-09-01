package com.todak_todag.provider_service.provider.presentation.api_controller;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCommand;
import com.todak_todag.provider_service.provider.application.command_service.ServiceOfferingCommandService;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingResult;
import com.todak_todag.provider_service.provider.presentation.request.ServiceOfferingRequest;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-offerings")
@RequiredArgsConstructor
public class ServiceOfferingApiController {

    private static final String SERVICE_PROVIDER = "SERVICE_PROVIDER";

    private final ServiceOfferingCommandService serviceOfferingCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ServiceOfferingResponse.Create> create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ServiceOfferingRequest.Create request
    ) {
        if (!SERVICE_PROVIDER.equals(userRole)) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        ServiceOfferingResult.Create result = serviceOfferingCommandService.create(
                new ServiceOfferingCommand.Create(userId, request.provideServiceId())
        );

        return ApiResponse.created("제공 서비스 등록 성공", ServiceOfferingResponse.Create.from(result));
    }
}