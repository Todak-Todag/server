package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanDeleteCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.application.facade.CarePlanFacade;
import com.spring.careplanservice.careplan.application.query.CarePlanFindQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanSearchQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.CarePlanFindResult;
import com.spring.careplanservice.careplan.application.result.CarePlanSearchResult;
import com.spring.careplanservice.careplan.application.result.CarePlanStatusUpdateResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.careplan.application.service.query.CarePlanQueryService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.presentation.request.CarePlanCreateRequest;
import com.spring.careplanservice.careplan.presentation.request.CarePlanStatusUpdateRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanCreateResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanFindResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanSearchResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanStatusUpdateResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import com.spring.careplanservice.global.response.PageResponse;
import com.spring.careplanservice.global.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/care-plans")
public class CarePlanController {
    private final CarePlanFacade carePlanFacade;
    private final CarePlanQueryService carePlanQueryService;
    private final CarePlanCommandService carePlanCommandService;

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

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PageResponse<CarePlanSearchResponse>>> searchCarePlan(
            @AuthenticationPrincipal UserContext user,
            @RequestParam(required = false) CarePlanStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate finishDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        CarePlanSearchQuery carePlanSearchQuery = new CarePlanSearchQuery(
                user.userId(),
                status,
                startDate,
                finishDate,
                page,
                size
        );

        Page<CarePlanSearchResult> resultPage = carePlanQueryService.searchCarePlan(
                carePlanSearchQuery
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Care Plan 목록 조회 성공",
                        PageResponse.of(resultPage, CarePlanSearchResponse::from)
                )
        );
    }

    @PatchMapping("/{carePlanId}/status")
    @PreAuthorize("hasAnyRole('SERVICE_PROVIDER', 'SOCIAL_WORKER', 'ADMIN', 'MASTER')")
    public ResponseEntity<ApiResponse<CarePlanStatusUpdateResponse>> updateCarePlanStatus(
            @AuthenticationPrincipal UserContext user,
            @PathVariable UUID carePlanId,
            @Valid @RequestBody CarePlanStatusUpdateRequest carePlanStatusUpdateRequest
    ) {
        CarePlanStatusUpdateCommand carePlanStatusUpdateCommand = carePlanStatusUpdateRequest.toCommand(
                user.userId(),
                user.role(),
                carePlanId
        );

        CarePlanStatusUpdateResult carePlanStatusUpdateResult = carePlanCommandService.updateCarePlanStatus(carePlanStatusUpdateCommand);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Care Plan 수정 성공",
                        CarePlanStatusUpdateResponse.from(carePlanStatusUpdateResult)
                )
        );
    }

    @DeleteMapping("/{carePlanId}")
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'ADMIN', 'MASTER')")
    public ResponseEntity<Void> deleteCarePlan(
            @AuthenticationPrincipal UserContext user,
            @PathVariable UUID carePlanId
    ) {
        carePlanCommandService.deleteCarePlan(
                new CarePlanDeleteCommand(
                        user.userId(),
                        carePlanId
                )
        );

        return ResponseEntity.noContent().build();
    }
}
