package com.todak_todag.discharge_service.discharge.presentation.controller.internal;

import com.todak_todag.discharge_service.discharge.presentation.response.DischargeInternalFindResponse;
import com.todak_todag.discharge_service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Tag(
        name = "Internal - Discharge",
        description = "퇴원 내부 API"
)
public interface DischargeInternalApiSpec {

    @Operation(
            summary = "퇴원건 내부 조회",
            description = """
                    내부 서비스가 퇴원건 ID를 기준으로 퇴원 정보를 조회한다.

                    서비스 간 내부 호출 전용 API이며 X-Internal-Api-Key 인증을 사용한다.
                    퇴원건 ID, 퇴원 예정자 ID, 실제 퇴원일을 반환한다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 내부 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "X-Internal-Api-Key 누락 또는 불일치"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "퇴원건을 찾을 수 없음"
            )
    })
    ApiResponse<DischargeInternalFindResponse> findById(
            @Parameter(
                    name = "dischargeId",
                    description = "조회할 퇴원건 ID",
                    required = true
            )
            UUID dischargeId
    );
}