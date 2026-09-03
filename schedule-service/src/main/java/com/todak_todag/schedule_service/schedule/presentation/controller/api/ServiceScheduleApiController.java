package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceScheduleFacade;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleCancelRequest;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleRescheduleRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleCancelResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleRescheduleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Controller는 헤더를 직접 읽지 않고 @AuthenticationPrincipal UserContext user로만 주입받음
@RestController
@RequestMapping("/api/v1/service-schedules")
@RequiredArgsConstructor
@Validated
public class ServiceScheduleApiController implements ScheduleApiSpec {

    private final ServiceScheduleFacade serviceScheduleFacade;

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
}
