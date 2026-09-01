package com.todak_todag.discharge_service.discharge.presentation.api_controller;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.command_service.DischargeCommandService;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.presentation.request.DischargeCreateRequest;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeCreateResponse;
import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import com.todak_todag.discharge_service.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discharges")
public class DischargeApiController {

    private final DischargeCommandService dischargeCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<DischargeCreateResponse>> createDischarge(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody DischargeCreateRequest request
    ) {
        validateHospitalStaff(userRole);

        DischargeCreateCommand command =
                new DischargeCreateCommand(
                        userId,
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

    private void validateHospitalStaff(
            String userRole
    ) {
        if (!"HOSPITAL_STAFF".equals(userRole)) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }
    }
}