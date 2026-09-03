package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleCancelRequest;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleRescheduleRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleCancelResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleRescheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Service Schedule", description = "서비스 일정 API")
public interface ScheduleApiSpec {

    @Operation(
            summary = "서비스 일정 변경",
            description = "퇴원 예정자가 본인에게 배정된 서비스 일정을 기존 날짜 기준 하루 앞당기거나 하루 미루도록 변경 요청한다. " +
                    "요청이 접수되면 status가 RESCHEDULING으로 바뀌고 ProviderReMatched 이벤트가 발행된다. " +
                    "재매칭 최종 결과(CHANGED 또는 SCHEDULED 복구)는 이 응답에 포함되지 않는다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServiceScheduleRescheduleResponse>> reschedule(
            @Parameter(name = "serviceScheduleId", description = "변경할 서비스 일정 ID", required = true)
            UUID serviceScheduleId,
            @Parameter(description = "변경할 일정 날짜", required = true)
            @Valid
            ServiceScheduleRescheduleRequest request,
            @Parameter(hidden = true)
            UserContext user
    );

    @Operation(
            summary = "서비스 일정 취소",
            description = "퇴원 예정자가 본인에게 배정된 서비스 일정을 취소한다. " +
                    "일정 시작 24시간 전까지만 가능하며, 취소 시 status가 CANCELED로 바뀌고 cancelReason/canceledAt이 기록된다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServiceScheduleCancelResponse>> cancel(
            @Parameter(name = "serviceScheduleId", description = "취소할 서비스 일정 ID", required = true)
            UUID serviceScheduleId,
            @Parameter(description = "취소 사유", required = true)
            @Valid
            ServiceScheduleCancelRequest request,
            @Parameter(hidden = true)
            UserContext user
    );
}
