package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.facade.CarePlanFacade;
import com.spring.careplanservice.careplan.application.query.CarePlanFindQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.CarePlanFindResult;
import com.spring.careplanservice.careplan.application.service.query.CarePlanQueryService;
import com.spring.careplanservice.careplan.presentation.request.CarePlanCreateRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanCreateResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanFindResponse;
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
@RequestMapping("/api/v1/care-plans")
public class CarePlanController {
    private final CarePlanFacade carePlanFacade;
    private final CarePlanQueryService carePlanQueryService;

    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'PATIENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<CarePlanCreateResponse>> createCarePlan(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody CarePlanCreateRequest carePlanCreateRequest
    ) {
        CarePlanCreateCommand carePlanCreateCommand = carePlanCreateRequest.toCommand(
                user.userId(),
                user.role()
        );

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

    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN', 'SOCIAL_WORKER', 'MASTER')")
    @GetMapping("/{carePlanId}")
    public ResponseEntity<ApiResponse<CarePlanFindResponse>> findCarePlan(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("carePlanId") UUID carePlanId
    ) {
        CarePlanFindQuery carePlanFindQuery = new CarePlanFindQuery(
                carePlanId,
                user.userId()
        );

        CarePlanFindResult carePlanFindResult = carePlanQueryService.findCarePlan(
                carePlanFindQuery
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Care Plan 상세 조회 성공",
                        CarePlanFindResponse.from(carePlanFindResult)
                )
        );
    }
}
