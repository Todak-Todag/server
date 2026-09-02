package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceScheduleCommandService;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleRescheduleRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleRescheduleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final ServiceScheduleCommandService serviceScheduleCommandService;

    // [외부 API] 서비스 일정 변경
    @Override
    @PatchMapping("/{serviceScheduleId}/status")
    public ResponseEntity<ApiResponse<ServiceScheduleRescheduleResponse>> reschedule(
            @PathVariable UUID serviceScheduleId,
            @Valid @RequestBody ServiceScheduleRescheduleRequest request,
            @AuthenticationPrincipal UserContext user
    ) {
        ServiceScheduleRescheduleResult result = serviceScheduleCommandService.reschedule(
                request.toCommand(serviceScheduleId, user.getUserId())
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 일정 변경 성공",
                                ServiceScheduleRescheduleResponse.from(result)
                        )
                );
    }
}
