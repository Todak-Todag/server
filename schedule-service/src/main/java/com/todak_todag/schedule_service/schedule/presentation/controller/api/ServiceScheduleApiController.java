package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.common.PageableFactory;
import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceScheduleFacade;
import com.todak_todag.schedule_service.schedule.application.query.ServiceScheduleDetailQuery;
import com.todak_todag.schedule_service.schedule.application.query.ServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleCancelRequest;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleCompleteRequest;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleRescheduleRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleCancelResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleCompleteResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleDetailResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleRescheduleResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

// Controller는 헤더를 직접 읽지 않고 @AuthenticationPrincipal UserContext user로만 주입받음
@RestController
@RequestMapping("/api/v1/service-schedules")
@RequiredArgsConstructor
@Validated
public class ServiceScheduleApiController implements ScheduleApiSpec {

    private final ServiceScheduleFacade serviceScheduleFacade;

    // [외부 API] 서비스 일정 목록 조회 — 퇴원 예정자/서비스 제공자 공용
    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<PageResponse<ServiceScheduleSearchResponse>>> search(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserContext user
    ) {
        // page/size/sort 보정은 PageableFactory에 위임
        Pageable pageable = PageableFactory.of(page, size, sort);

        // status는 Spring 기본 Enum 컨버터를 쓰지 않고 String으로 받아 ScheduleStatus.fromFilter에서 직접 파싱
        ScheduleStatus statusFilter = ScheduleStatus.fromFilter(status);

        ServiceScheduleSearchQuery searchQuery = new ServiceScheduleSearchQuery(
                user.getUserId(),
                user.getRole(),
                statusFilter,
                date,
                pageable
        );

        Page<ServiceScheduleSearchResult> result = serviceScheduleFacade.search(searchQuery);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 일정 목록 조회 성공",
                                PageResponse.of(result, ServiceScheduleSearchResponse::from)
                        )
                );
    }

    // [외부 API] 서비스 일정 상세 조회 — 퇴원 예정자/서비스 제공자 공용
    @Override
    @GetMapping("/{serviceScheduleId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<ServiceScheduleDetailResponse>> detail(
            @PathVariable UUID serviceScheduleId,
            @AuthenticationPrincipal UserContext user
    ) {
        ServiceScheduleDetailQuery detailQuery = new ServiceScheduleDetailQuery(
                serviceScheduleId, user.getUserId(), user.getRole()
        );

        ServiceScheduleDetailResult result = serviceScheduleFacade.detail(detailQuery);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 일정 상세 조회 성공",
                                ServiceScheduleDetailResponse.from(result)
                        )
                );
    }

    // [외부 API] 서비스 일정 변경 — 퇴원 예정자 전용
    @Override
    @PatchMapping("/{serviceScheduleId}/status")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<ServiceScheduleRescheduleResponse>> reschedule(
            @PathVariable UUID serviceScheduleId,
            @Valid @RequestBody ServiceScheduleRescheduleRequest rescheduleRequest,
            @AuthenticationPrincipal UserContext user
    ) {
        ServiceScheduleRescheduleResult rescheduleResult = serviceScheduleFacade.reschedule(
                rescheduleRequest.toCommand(serviceScheduleId, user.getUserId())
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 일정 변경 성공",
                                ServiceScheduleRescheduleResponse.from(rescheduleResult)
                        )
                );
    }

    // [외부 API] 서비스 일정 취소 — 퇴원 예정자 전용
    @Override
    @PatchMapping("/{serviceScheduleId}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<ServiceScheduleCancelResponse>> cancel(
            @PathVariable UUID serviceScheduleId,
            @Valid @RequestBody ServiceScheduleCancelRequest cancelRequest,
            @AuthenticationPrincipal UserContext user
    ) {
        ServiceScheduleCancelResult cancelResult = serviceScheduleFacade.cancel(
                cancelRequest.toCommand(serviceScheduleId, user.getUserId())
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 일정 취소 성공",
                                ServiceScheduleCancelResponse.from(cancelResult)
                        )
                );
    }

    // [외부 API] 서비스 수행 완료 — 서비스 제공자 전용
    @Override
    @PatchMapping("/{serviceScheduleId}/result")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<ServiceScheduleCompleteResponse>> complete(
            @PathVariable UUID serviceScheduleId,
            @Valid @RequestBody ServiceScheduleCompleteRequest completeRequest,
            @AuthenticationPrincipal UserContext user
    ) {
        ServiceScheduleCompleteResult completeResult = serviceScheduleFacade.complete(
                completeRequest.toCommand(serviceScheduleId, user.getUserId())
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 수행 완료 상태 변경 성공",
                                ServiceScheduleCompleteResponse.from(completeResult)
                        )
                );
    }
}
