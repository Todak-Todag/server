package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleCancelRequest;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleCompleteRequest;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceScheduleRescheduleRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleCancelResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleCompleteResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleDetailResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleRescheduleResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceScheduleSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Service Schedule", description = "서비스 일정 API")
public interface ScheduleApiSpec {

    @Operation(
            summary = "서비스 일정 목록 조회",
            description = "퇴원 예정자는 본인이 받은, 서비스 제공자는 본인이 제공한 서비스 일정 목록을 조회한다. " +
                    "status/date로 필터링할 수 있으며 정렬은 최신순/오래된순(기본 createdAt,DESC)이 가능하다. " +
                    "status에 허용되지 않는 값이 들어오면 400을 반환한다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<PageResponse<ServiceScheduleSearchResponse>>> search(
            @Parameter(description = "페이지 번호 (기본 0)")
            Integer page,
            @Parameter(description = "페이지 크기 (10/30/50, 이외 값은 10으로 자동 보정, 기본 10)")
            Integer size,
            @Parameter(description = "정렬 (기본 createdAt,DESC)")
            String sort,
            @Parameter(description = "상태 필터 (SCHEDULED/RESCHEDULING/CHANGED/COMPLETED/CANCELED/NO_SHOW)")
            String status,
            @Parameter(description = "날짜 필터")
            LocalDate date,
            @Parameter(hidden = true)
            UserContext user
    );

    @Operation(
            summary = "서비스 일정 상세 조회",
            description = "퇴원 예정자는 본인이 받은, 서비스 제공자는 본인이 제공한 서비스 일정 하나의 상세 정보를 조회한다. " +
                    "취소되지 않은 일정은 cancelReason/canceledAt이 null로 반환된다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServiceScheduleDetailResponse>> detail(
            @Parameter(name = "serviceScheduleId", description = "조회할 서비스 일정 ID", required = true)
            UUID serviceScheduleId,
            @Parameter(hidden = true)
            UserContext user
    );

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

    @Operation(
            summary = "서비스 수행 완료",
            description = "서비스 제공자가 본인에게 배정된 서비스 일정의 수행을 완료(COMPLETED) 또는 미완료(NO_SHOW)로 처리한다. " +
                    "status가 SCHEDULED인 일정만 대상이며, 요청 시점이 finishedAt 이후여야 한다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServiceScheduleCompleteResponse>> complete(
            @Parameter(name = "serviceScheduleId", description = "수행을 완료할 서비스 일정 ID", required = true)
            UUID serviceScheduleId,
            @Parameter(description = "수행 완료 상태 (COMPLETED 또는 NO_SHOW)", required = true)
            @Valid
            ServiceScheduleCompleteRequest request,
            @Parameter(hidden = true)
            UserContext user
    );
}
