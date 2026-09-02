package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.application.command.CarePlanServiceSelectCommand;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSelectResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanServiceCommandService;
import com.spring.careplanservice.careplan.presentation.request.CarePlanServiceSelectRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanServiceSelectResponse;
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
public class CarePlanServiceController {
    private final CarePlanServiceCommandService carePlanServiceCommandService;

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/care-plans/{carePlanId}/services")
    public ResponseEntity<ApiResponse<CarePlanServiceSelectResponse>> selectCarePlanService(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("carePlanId") UUID carePlanId,
            @Valid @RequestBody CarePlanServiceSelectRequest carePlanServiceSelectRequest
    ) {
        CarePlanServiceSelectCommand carePlanServiceSelectCommand = carePlanServiceSelectRequest.toCommand(
                user.userId(),
                carePlanId
        );

        CarePlanServiceSelectResult carePlanServiceSelectResult = carePlanServiceCommandService.selectCarePlanService(
                carePlanServiceSelectCommand
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                "Care Plan 서비스 선택 성공",
                                CarePlanServiceSelectResponse.from(
                                        carePlanServiceSelectResult
                                )
                        )
                );
    }
}
