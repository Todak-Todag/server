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
                    "존재하지 않거나 논리 삭제된 경우 404를 반환한다. " +
                    "수신 측이 '그 결과가 이벤트의 carePlanId 소속인지' 교차 검증할 수 있도록 carePlanId를 함께 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서비스 수행 결과 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "serviceResultId가 UUID 형식이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "X-Internal-Api-Key 없거나 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 논리 삭제된 수행 결과 / 조인 대상 일정이 없어 carePlanId를 확보할 수 없음")
    })
    ResponseEntity<ApiResponse<InternalServiceResultResponse>> detail(
            @Parameter(name = "serviceResultId", description = "조회할 서비스 수행 결과 ID", required = true)
            UUID serviceResultId
    );
}
