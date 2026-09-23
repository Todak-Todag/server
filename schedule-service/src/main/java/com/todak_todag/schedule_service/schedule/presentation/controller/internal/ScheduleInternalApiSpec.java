package com.todak_todag.schedule_service.schedule.presentation.controller.internal;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.InternalServiceScheduleListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Internal - Service Schedule", description = "서비스 일정 내부 API")
public interface ScheduleInternalApiSpec {

    @Operation(
            summary = "서비스 제공자 일정 조회",
            description = "Provider-Service가 매칭 가능 여부 판단을 위해, serviceOfferingIds에 대해 " +
                    "startDate부터 30일간(고정값)의 SCHEDULED/RESCHEDULING 상태 일정을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서비스 제공자 일정 조회 성공 (없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "serviceOfferingIds 누락/빈 값 또는 startDate 누락/형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "X-Internal-Api-Key 없거나 불일치")
    })
    ResponseEntity<ApiResponse<InternalServiceScheduleListResponse>> search(
            @Parameter(name = "serviceOfferingIds", description = "조회할 서비스 제공 항목 ID 목록 (콤마 구분)", required = true)
            @NotEmpty
            List<UUID> serviceOfferingIds,
            @Parameter(name = "startDate", description = "조회 시작 날짜 (여기서 30일간 조회)", required = true)
            LocalDate startDate
    );
}
