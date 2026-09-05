package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkUpdateCommand;
import com.todak_todag.provider_service.provider.application.facade.ServiceOfferingFacade;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkUpdateResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideWorkCommandService;
import com.todak_todag.provider_service.provider.presentation.request.ProvideWorkCreateRequest;
import com.todak_todag.provider_service.provider.presentation.request.ProvideWorkUpdateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ProvideWorkCreateResponse;
import com.todak_todag.provider_service.provider.presentation.response.ProvideWorkUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-offerings/{serviceOfferingId}/provide-works")
@RequiredArgsConstructor
public class ProvideWorkApiController {

    // 등록은 외부 호출이 없어 CommandService를 직접 사용하고,
    // 수정은 확정 일정 확인이 필요해 Facade를 거침
    private final ProvideWorkCommandService provideWorkCommandService;
    private final ServiceOfferingFacade serviceOfferingFacade;

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

    @PatchMapping("/{provideWorkId}")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ApiResponse<ProvideWorkUpdateResponse> update(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("serviceOfferingId") UUID serviceOfferingId,
            @PathVariable("provideWorkId") UUID provideWorkId,
            @Valid @RequestBody ProvideWorkUpdateRequest request
    ) {
        ProvideWorkUpdateResult result = serviceOfferingFacade.updateProvideWork(
                new ProvideWorkUpdateCommand(
                        serviceOfferingId,
                        provideWorkId,
                        user.getUserId(),
                        request.day(),
                        request.startedAt(),
                        request.finishedAt()
                )
        );

        return ApiResponse.ok("제공 가능 일정 수정 성공", ProvideWorkUpdateResponse.from(result));
    }
}