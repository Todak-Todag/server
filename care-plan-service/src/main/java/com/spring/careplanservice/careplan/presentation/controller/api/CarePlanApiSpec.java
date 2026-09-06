package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.presentation.request.CarePlanCreateRequest;
import com.spring.careplanservice.careplan.presentation.request.CarePlanStatusUpdateRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanCreateResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanFindResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanSearchResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanStatusUpdateResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import com.spring.careplanservice.global.response.PageResponse;
import com.spring.careplanservice.global.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Tag(
        name = "Care Plan",
        description = "Care Plan API"
)
public interface CarePlanApiSpec {
    @Operation(
            summary = "Care Plan 생성",
            description = """
                    퇴원이 완료된 퇴원 건을 기준으로 Care Plan을 생성한다.
                    
                    퇴원 예정자가 직접 생성하는 경우 본인의 Care Plan만 생성할 수 있다.
                    동일한 퇴원 건에 Care Plan을 중복 생성할 수 없다.
                    서비스 종류를 함께 전달하면 Care Plan 생성 시 신청 서비스도 함께 등록한다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<CarePlanCreateResponse>> createCarePlan(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(description = "Care Plan 생성 정보", required = true)
            @Valid
            CarePlanCreateRequest carePlanCreateRequest
    );

    @Operation(
            summary = "Care Plan 상세 조회",
            description = "Care Plan ID를 기준으로 Care Plan 상세 정보를 조회한다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<CarePlanFindResponse>> findCarePlan(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "carePlanId",
                    description = "조회할 Care Plan ID",
                    required = true
            )
            UUID carePlanId
    );

    @Operation(
            summary = "Care Plan 목록 조회",
            description = """
                    퇴원 예정자가 본인의 Care Plan 목록을 조회한다.
                    
                    상태와 Care Plan 시작일, 종료일을 기준으로 필터링할 수 있다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<PageResponse<CarePlanSearchResponse>>> searchCarePlan(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(description = "Care Plan 상태 필터")
            CarePlanStatus status,

            @Parameter(description = "Care Plan 시작일 필터")
            LocalDate startDate,

            @Parameter(description = "Care Plan 종료일 필터")
            LocalDate finishDate,

            @Parameter(description = "페이지 번호")
            Integer page,

            @Parameter(description = "페이지 크기")
            Integer size
    );

    @Operation(
            summary = "Care Plan 상태 변경",
            description = """
                    Care Plan 상태를 변경한다.
                    
                     현재 Care Plan 상태에서 허용된 다음 상태로만 변경할 수 있다.
                     퇴원 예정자는 UNDER_REVIEW → CONFIRMED,
                     서비스 제공자는 CONFIRMED → IN_PROGRESS 상태 변경을 수행한다.
                     COMPLETED 상태는 Schedule-Service의 완료 이벤트를 통해서만 변경된다.
                    
                     CONFIRMED 상태로 변경되면 서비스 제공자 매칭을 위한
                     Care Plan 확정 이벤트를 발행한다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<CarePlanStatusUpdateResponse>> updateCarePlanStatus(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "carePlanId",
                    description = "상태를 변경할 Care Plan ID",
                    required = true
            )
            UUID carePlanId,

            @Parameter(description = "변경할 Care Plan 상태", required = true)
            @Valid
            CarePlanStatusUpdateRequest carePlanStatusUpdateRequest
    );

    @Operation(
            summary = "Care Plan 삭제",
            description = """
                    Care Plan을 논리 삭제한다.
                    
                    UNDER_REVIEW 상태의 Care Plan만 삭제할 수 있다.
                    삭제 시 Care Plan에 포함된 서비스와 서비스 희망 일정도 함께 논리 삭제한다.
                    """
    )
    @ApiResponses
    ResponseEntity<Void> deleteCarePlan(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "carePlanId",
                    description = "삭제할 Care Plan ID",
                    required = true
            )
            UUID carePlanId
    );


}
