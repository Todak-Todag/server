package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceResultRegisterRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceResultRegisterResponse;
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
}
