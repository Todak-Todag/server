package com.spring.careplanservice.careplan.presentation.controller.api;


import com.spring.careplanservice.careplan.application.command.ServicePreferenceCreateCommand;
import com.spring.careplanservice.careplan.application.command.ServicePreferenceUpdateCommand;
import com.spring.careplanservice.careplan.application.query.ServicePreferenceFindQuery;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceCreateResult;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceFindResult;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceUpdateResult;
import com.spring.careplanservice.careplan.application.service.command.ServicePreferenceCommandService;
import com.spring.careplanservice.careplan.application.service.query.ServicePreferenceQueryService;
import com.spring.careplanservice.careplan.presentation.request.ServicePreferenceCreateRequest;
import com.spring.careplanservice.careplan.presentation.request.ServicePreferenceUpdateRequest;
import com.spring.careplanservice.careplan.presentation.response.ServicePreferenceCreateResponse;
import com.spring.careplanservice.careplan.presentation.response.ServicePreferenceFindResponse;
import com.spring.careplanservice.careplan.presentation.response.ServicePreferenceUpdateResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import com.spring.careplanservice.global.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ServicePreferenceController {
    private final ServicePreferenceCommandService servicePreferenceCommandService;
    private final ServicePreferenceQueryService servicePreferenceQueryService;

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping(
            "/care-plan-services/{planServiceId}/service-preferences"
    )
    public ResponseEntity<ApiResponse<ServicePreferenceCreateResponse>>
    createServicePreference(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("planServiceId") UUID planServiceId,
            @Valid @RequestBody
            ServicePreferenceCreateRequest servicePreferenceCreateRequest
    ) {

        ServicePreferenceCreateCommand servicePreferenceCreateCommand =
                servicePreferenceCreateRequest.toCommand(
                        user.userId(),
                        planServiceId
                );

        ServicePreferenceCreateResult servicePreferenceCreateResult = servicePreferenceCommandService.createServicePreference(servicePreferenceCreateCommand);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                "서비스 희망 일정 등록 성공",
                                ServicePreferenceCreateResponse.from(
                                        servicePreferenceCreateResult
                                )
                        )
                );
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PatchMapping(
            "/service-preferences/{servicePreferenceId}"
    )
    public ResponseEntity<ApiResponse<ServicePreferenceUpdateResponse>>
    updateServicePreference(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("servicePreferenceId") UUID servicePreferenceId,
            @Valid @RequestBody
            ServicePreferenceUpdateRequest servicePreferenceUpdateRequest
    ) {

        ServicePreferenceUpdateCommand servicePreferenceUpdateCommand =
                servicePreferenceUpdateRequest.toCommand(
                        user.userId(),
                        servicePreferenceId
                );

        ServicePreferenceUpdateResult servicePreferenceUpdateResult = servicePreferenceCommandService.updateServicePreference(servicePreferenceUpdateCommand);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "서비스 희망 일정 수정",
                        ServicePreferenceUpdateResponse.from(
                                servicePreferenceUpdateResult
                        )
                )
        );
    }

    // TODO: (MVP 이후) HOSPITAL_STAFF/SOCIAL_WORKER 관계 검증 API 연동 후 hasAnyRole("PATIENT", "HOSPITAL_STAFF", "SOCIAL_WORKER")로 확장
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping(
            "/service-preferences/{servicePreferenceId}"
    )
    public ResponseEntity<ApiResponse<ServicePreferenceFindResponse>>
    findServicePreference(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("servicePreferenceId") UUID servicePreferenceId
    ) {
        ServicePreferenceFindQuery servicePreferenceFindQuery = new ServicePreferenceFindQuery(
                user.userId(),
                user.role(),
                servicePreferenceId
        );

        ServicePreferenceFindResult servicePreferenceFindResult = servicePreferenceQueryService.findServicePreference(
                servicePreferenceFindQuery
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "서비스 희망 일정 상세 조회 성공",
                        ServicePreferenceFindResponse.from(
                                servicePreferenceFindResult
                        )
                )
        );
    }
}
