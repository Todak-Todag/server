package com.todak_todag.discharge_service.discharge.presentation.api_controller;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.command_service.DischargeCommandService;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.presentation.request.DischargeCreateRequest;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeCreateResponse;
import com.todak_todag.discharge_service.global.response.ApiResponse;
import com.todak_todag.discharge_service.global.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discharges")
public class DischargeApiController {

    private final DischargeCommandService dischargeCommandService;

    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PostMapping
    public ResponseEntity<ApiResponse<DischargeCreateResponse>> createDischarge(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody DischargeCreateRequest request
    ) {

        DischargeCreateCommand command =
                new DischargeCreateCommand(
                        user.getUserId(),
                        request.patientId(),
                        request.hospitalName(),
                        request.scheduledDate()
                );

        DischargeCreateResult result =
                dischargeCommandService.createDischarge(command);

        DischargeCreateResponse response =
                DischargeCreateResponse.from(result);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "퇴원건이 생성되었습니다.",
                                response
                        )
                );
    }
}