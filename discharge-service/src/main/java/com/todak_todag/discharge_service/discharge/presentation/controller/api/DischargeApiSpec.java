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
import jakarta.validation.Valid;
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
            description = """
                    병원 담당자가 퇴원 예정자의 퇴원 정보를 생성한다.

                    퇴원 예정일은 요청일 이후의 날짜만 등록할 수 있다.
                    생성된 퇴원건의 초기 상태는 SCHEDULED이다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "퇴원건 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 오류 또는 유효하지 않은 퇴원 예정일"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 오류"
            )
    })
    ResponseEntity<ApiResponse<DischargeCreateResponse>> createDischarge(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    description = "퇴원건 생성 정보",
                    required = true
            )
            @Valid
            DischargeCreateRequest request
    );

    @Operation(
            summary = "퇴원건 목록 조회",
            description = """
                    병원 담당자가 자신이 등록한 퇴원건 목록을 조회한다.

                    퇴원 상태와 퇴원 예정일을 기준으로 필터링할 수 있다.
                    정렬 기준은 생성일(createdAt)이며 기본 정렬 방향은 DESC이다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "조회 조건 형식 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 오류"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<DischargeSearchResponse>>> searchDischarges(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "page",
                    description = "페이지 번호 (0부터 시작, 음수는 0으로 보정)"
            )
            Integer page,

            @Parameter(
                    name = "size",
                    description = "페이지 크기 (10, 30, 50 중 하나. 그 외 값은 10으로 보정)"
            )
            Integer size,

            @Parameter(
                    name = "sort",
                    description = "생성일 정렬 방향 (예: createdAt,asc / createdAt,desc, 기본 DESC)"
            )
            String sort,

            @Parameter(
                    name = "status",
                    description = "퇴원 상태 필터"
            )
            DischargeStatus status,

            @Parameter(
                    name = "scheduledDate",
                    description = "퇴원 예정일 필터"
            )
            LocalDate scheduledDate
    );

    @Operation(
            summary = "퇴원건 수정",
            description = """
                    병원 담당자가 자신이 등록한 퇴원건의 상태 또는 퇴원 예정일을 수정한다.

                    status와 scheduledDate 중 하나 이상을 입력해야 한다.
                    SCHEDULED 상태에서는 POSTPONED 또는 CANCELED로 변경할 수 있다.
                    POSTPONED 상태에서는 CANCELED로 변경할 수 있다.
                    COMPLETED 또는 CANCELED 상태의 퇴원건은 수정할 수 없다.
                    POSTPONED 상태로 변경하는 경우 변경할 퇴원 예정일은 필수이다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 수정 요청, 유효하지 않은 예정일 또는 허용되지 않은 상태 변경"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "퇴원건 수정 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "퇴원건을 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<DischargeUpdateResponse>> updateDischarge(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "dischargeId",
                    description = "수정할 퇴원건 ID",
                    required = true
            )
            UUID dischargeId,

            @Parameter(
                    description = "퇴원건 수정 정보",
                    required = true
            )
            @Valid
            DischargeUpdateRequest request
    );

    @Operation(
            summary = "퇴원 완료 처리",
            description = """
                    병원 담당자가 자신이 등록한 퇴원건을 완료 처리한다.

                    SCHEDULED 또는 POSTPONED 상태의 퇴원건만 완료할 수 있다.
                    실제 퇴원일은 필수이며 미래 날짜를 입력할 수 없다.
                    완료 처리된 퇴원건의 상태는 COMPLETED로 변경된다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원 완료 처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 실제 퇴원일 또는 완료할 수 없는 상태"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "퇴원 완료 처리 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "퇴원건을 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<DischargeCompleteResponse>> completeDischarge(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "dischargeId",
                    description = "완료 처리할 퇴원건 ID",
                    required = true
            )
            UUID dischargeId,

            @Parameter(
                    description = "퇴원 완료 처리 정보",
                    required = true
            )
            @Valid
            DischargeCompleteRequest request
    );

    @Operation(
            summary = "퇴원건 상세 조회",
            description = """
                    퇴원건 ID를 기준으로 퇴원건 상세 정보를 조회한다.

                    병원 담당자와 퇴원 예정자가 조회할 수 있다.
                    사용자 역할과 요청자 정보를 기준으로 조회 권한을 검증한다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "퇴원건 상세 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "조회 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "퇴원건을 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<DischargeFindResponse>> findDischarge(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "dischargeId",
                    description = "조회할 퇴원건 ID",
                    required = true
            )
            UUID dischargeId
    );
}