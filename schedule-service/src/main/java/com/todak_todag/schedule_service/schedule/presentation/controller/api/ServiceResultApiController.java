package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.common.PageableFactory;
import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceResultFacade;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultDetailQuery;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultSearchResult;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceResultRegisterRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceResultDetailResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceResultRegisterResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceResultSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Controller는 헤더를 직접 읽지 않고 @AuthenticationPrincipal UserContext user로만 주입받음
@RestController
@RequestMapping("/api/v1/service-results")
@RequiredArgsConstructor
@Validated
public class ServiceResultApiController implements ServiceResultApiSpec {

    private final ServiceResultFacade serviceResultFacade;

    // [외부 API] 서비스 수행 결과 등록 — 서비스 제공자 전용
    @Override
    @PostMapping("/{serviceScheduleId}")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<ServiceResultRegisterResponse>> register(
            @PathVariable UUID serviceScheduleId,
            @Valid @RequestBody ServiceResultRegisterRequest request,
            @AuthenticationPrincipal UserContext user
    ) {
        ServiceResultRegisterResult result = serviceResultFacade.register(
                request.toCommand(serviceScheduleId, user.getUserId())
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "서비스 수행 결과 등록 성공",
                                ServiceResultRegisterResponse.from(result)
                        )
                );
    }

    // [외부 API] 서비스 수행 결과 목록 조회 — 퇴원 예정자/서비스 제공자 공용
    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<PageResponse<ServiceResultSearchResponse>>> search(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserContext user
    ) {
        // page/size/sort 보정은 PageableFactory에 위임
        Pageable pageable = PageableFactory.of(page, size, sort);

        ServiceResultSearchQuery searchQuery = new ServiceResultSearchQuery(
                user.getUserId(),
                user.getRole(),
                pageable
        );

        Page<ServiceResultSearchResult> result = serviceResultFacade.search(searchQuery);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 수행 결과 목록 조회 성공",
                                PageResponse.of(result, ServiceResultSearchResponse::from)
                        )
                );
    }

    // [외부 API] 서비스 수행 결과 상세 조회 — 퇴원 예정자/서비스 제공자 공용
    @Override
    @GetMapping("/{serviceResultId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<ServiceResultDetailResponse>> detail(
            @PathVariable UUID serviceResultId,
            @AuthenticationPrincipal UserContext user
    ) {
        ServiceResultDetailQuery detailQuery = new ServiceResultDetailQuery(
                serviceResultId, user.getUserId(), user.getRole()
        );

        ServiceResultDetailResult result = serviceResultFacade.detail(detailQuery);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 수행 결과 상세 조회 성공",
                                ServiceResultDetailResponse.from(result)
                        )
                );
    }
}
