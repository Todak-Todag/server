package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.application.command.CarePlanServiceCancelCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanServiceSelectCommand;
import com.spring.careplanservice.careplan.application.query.CarePlanServiceSearchQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSearchResult;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSelectResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanServiceCommandService;
import com.spring.careplanservice.careplan.application.service.query.CarePlanServiceQueryService;
import com.spring.careplanservice.careplan.presentation.request.CarePlanServiceSelectRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanServiceSearchResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanServiceSelectResponse;
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

import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CarePlanServiceController {
    private final CarePlanServiceCommandService carePlanServiceCommandService;
    private final CarePlanServiceQueryService carePlanServiceQueryService;

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

    @PreAuthorize("hasRole('PATIENT')")
    @DeleteMapping("/care-plan-services/{planServiceId}")
    public ResponseEntity<Void> cancelCarePlanService(
            @AuthenticationPrincipal UserContext user,
            @PathVariable("planServiceId") UUID planServiceId
    ) {
        carePlanServiceCommandService.cancelCarePlanService(
                new CarePlanServiceCancelCommand(
                        user.userId(),
                        planServiceId
                )
        );

        return ResponseEntity.noContent().build();
    }

    // TODO: (MVP 이후) HOSPITAL_STAFF, SOCIAL_WORKER 조회 권한 확장
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/care-plans/{carePlanId}/services")
    public ResponseEntity<ApiResponse<PageResponse<CarePlanServiceSearchResponse>>> searchCarePlanServices(
            @AuthenticationPrincipal UserContext user,
            @PathVariable UUID carePlanId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        CarePlanServiceSearchQuery carePlanServiceSearchQuery = new CarePlanServiceSearchQuery(
                user.userId(),
                carePlanId,
                page,
                size
        );

        Page<CarePlanServiceSearchResult> resultPage = carePlanServiceQueryService.searchCarePlanServices(
                carePlanServiceSearchQuery
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "신청 서비스 목록 조회 성공",
                        PageResponse.of(resultPage, CarePlanServiceSearchResponse::from)
                )
        );
    }
}
