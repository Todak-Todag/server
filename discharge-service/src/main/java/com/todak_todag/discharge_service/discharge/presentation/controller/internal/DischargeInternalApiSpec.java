package com.todak_todag.discharge_service.discharge.presentation.controller.internal;

import com.todak_todag.discharge_service.discharge.presentation.response.DischargeInternalFindResponse;
import com.todak_todag.discharge_service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Tag(
        name = "Discharge Internal",
        description = "서비스 간 내부 호출용 퇴원 API"
)
public interface DischargeInternalApiSpec {

    @Operation(
            summary = "퇴원건 내부 조회",
            description = "내부 서비스가 dischargeId를 기준으로 퇴원건 정보를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 내부 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "내부 API 인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "퇴원건을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ApiResponse<DischargeInternalFindResponse> findById(
            @Parameter(
                    description = "퇴원건 ID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            UUID dischargeId
    );
}