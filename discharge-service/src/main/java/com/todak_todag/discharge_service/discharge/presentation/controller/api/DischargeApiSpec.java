package com.todak_todag.discharge_service.discharge.presentation.controller.api;

import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import com.todak_todag.discharge_service.discharge.presentation.request.DischargeCompleteRequest;
import com.todak_todag.discharge_service.discharge.presentation.request.DischargeCreateRequest;
import com.todak_todag.discharge_service.discharge.presentation.request.DischargeUpdateRequest;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeCompleteResponse;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeCreateResponse;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeFindResponse;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeSearchResponse;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeUpdateResponse;
import com.todak_todag.discharge_service.global.response.ApiResponse;
import com.todak_todag.discharge_service.global.response.PageResponse;
import com.todak_todag.discharge_service.global.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Tag(
        name = "Discharge",
        description = "퇴원 관리 API"
)
public interface DischargeApiSpec {

    @Operation(
            summary = "퇴원건 생성",
            description = "병원 담당자가 퇴원 예정자의 퇴원 정보를 생성한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "퇴원건 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 입력값 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<DischargeCreateResponse>> createDischarge(
            @Parameter(hidden = true)
            UserContext user,

            DischargeCreateRequest request
    );

    @Operation(
            summary = "퇴원건 목록 조회",
            description = "병원 담당자가 자신이 작성한 퇴원건 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 조회 조건"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<DischargeSearchResponse>>> searchDischarges(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    description = "페이지 번호",
                    example = "0"
            )
            Integer page,

            @Parameter(
                    description = "페이지 크기",
                    example = "10"
            )
            Integer size,

            @Parameter(
                    description = "정렬 조건",
                    example = "scheduledDate,desc"
            )
            String sort,

            @Parameter(
                    description = "퇴원 상태",
                    example = "SCHEDULED"
            )
            DischargeStatus status,

            @Parameter(
                    description = "퇴원 예정일",
                    example = "2026-09-30"
            )
            LocalDate scheduledDate
    );

    @Operation(
            summary = "퇴원건 수정",
            description = "병원 담당자가 자신이 작성한 퇴원건의 상태 또는 퇴원 예정일을 수정한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 수정 요청 또는 허용되지 않은 상태 변경"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 오류"
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
    ResponseEntity<ApiResponse<DischargeUpdateResponse>> updateDischarge(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    description = "퇴원건 ID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            UUID dischargeId,

            DischargeUpdateRequest request
    );

    @Operation(
            summary = "퇴원 완료 처리",
            description = "병원 담당자가 퇴원 예정자를 실제 퇴원 완료 상태로 변경한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원 완료 처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 완료 요청 또는 허용되지 않은 상태 변경"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 오류"
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
    ResponseEntity<ApiResponse<DischargeCompleteResponse>> completeDischarge(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    description = "퇴원건 ID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            UUID dischargeId,

            DischargeCompleteRequest request
    );

    @Operation(
            summary = "퇴원건 단건 조회",
            description = "병원 담당자 또는 퇴원 예정자가 특정 퇴원건의 상세 정보를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 오류"
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
    ResponseEntity<ApiResponse<DischargeFindResponse>> findDischarge(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    description = "퇴원건 ID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            UUID dischargeId
    );
}