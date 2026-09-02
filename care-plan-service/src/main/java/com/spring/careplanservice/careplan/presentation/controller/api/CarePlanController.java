package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.facade.CarePlanFacade;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.presentation.request.CarePlanCreateRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanCreateResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import com.spring.careplanservice.global.security.UserContext;
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
@RequestMapping("/api/v1")
public class CarePlanController {
    private final CarePlanFacade carePlanFacade;

    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'PATIENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<CarePlanCreateResponse>> createCarePlan(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody CarePlanCreateRequest carePlanCreateRequest
    ) {
        CarePlanCreateCommand carePlanCreateCommand = carePlanCreateRequest.toCommand(user.userId());

        CarePlanCreateResult carePlanCreateResult = carePlanFacade.createCarePlan(
                carePlanCreateCommand
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                "Care Plan 생성 성공",
                                CarePlanCreateResponse.from(carePlanCreateResult)
                        ));
    }
}
