package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceResultRegisterRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceResultRegisterResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceResultSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Service Result", description = "서비스 수행 결과 API")
public interface ServiceResultApiSpec {

    @Operation(
            summary = "서비스 수행 결과 등록",
            description = "서비스 제공자가 본인에게 배정된 서비스 일정의 수행 결과(실제 시작·종료 일시, 비고)를 등록한다. " +
                    "대상 일정의 status가 COMPLETED 또는 NO_SHOW로 확정된 이후에만 등록할 수 있으며, " +
                    "동일 일정에 대한 중복 등록은 허용하지 않는다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServiceResultRegisterResponse>> register(
            @Parameter(name = "serviceScheduleId", description = "결과를 등록할 서비스 일정 ID", required = true)
            UUID serviceScheduleId,
            @Parameter(description = "실제 시작/종료 일시와 비고", required = true)
            @Valid
            ServiceResultRegisterRequest request,
            @Parameter(hidden = true)
            UserContext user
    );

    @Operation(
            summary = "서비스 수행 결과 목록 조회",
            description = "퇴원 예정자는 본인이 받은, 서비스 제공자는 본인이 제공한 서비스의 수행 결과 목록을 조회한다. " +
                    "정렬은 최신순/오래된순(기본 createdAt,DESC)이 가능하며, 조회 결과가 없으면 빈 배열을 반환한다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<PageResponse<ServiceResultSearchResponse>>> search(
            @Parameter(description = "페이지 번호 (기본 0)")
            Integer page,
            @Parameter(description = "페이지 크기 (10/30/50, 이외 값은 10으로 자동 보정, 기본 10)")
            Integer size,
            @Parameter(description = "정렬 (기본 createdAt,DESC)")
            String sort,
            @Parameter(hidden = true)
            UserContext user
    );
}
