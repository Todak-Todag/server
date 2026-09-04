package com.todak_todag.schedule_service.schedule.presentation.controller.internal;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.InternalServiceResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Internal - Service Result", description = "서비스 수행 결과 내부 API")
public interface ServiceResultInternalApiSpec {

    @Operation(
            summary = "서비스 수행 결과 조회",
            description = "Care-Plan-Service가 CarePlanCompleted 이벤트 페이로드에 담긴 serviceResultId가 " +
                    "실제로 존재하는 데이터인지 검증하기 위해 호출한다. deletedAt IS NULL인 수행 결과만 반환하며, " +
                    "존재하지 않거나 논리 삭제된 경우 404를 반환한다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<InternalServiceResultResponse>> detail(
            @Parameter(name = "serviceResultId", description = "조회할 서비스 수행 결과 ID", required = true)
            UUID serviceResultId
    );
}
