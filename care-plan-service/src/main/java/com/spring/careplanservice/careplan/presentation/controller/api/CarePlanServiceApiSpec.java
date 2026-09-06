package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.presentation.request.CarePlanServiceSelectRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanServiceFindResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanServiceSearchResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanServiceSelectResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import com.spring.careplanservice.global.response.PageResponse;
import com.spring.careplanservice.global.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(
        name = "Care Plan Service",
        description = "Care Plan 신청 서비스 API"
)
public interface CarePlanServiceApiSpec {

    @Operation(
            summary = "Care Plan 서비스 신청",
            description = """
                    퇴원 예정자가 Care Plan에 제공받을 서비스를 추가한다.
                    
                    Care Plan에 추가할 서비스 종류 ID를 전달하여
                    새로운 Care Plan 서비스 항목을 생성한다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<CarePlanServiceSelectResponse>> selectCarePlanService(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "carePlanId",
                    description = "서비스를 추가할 Care Plan ID",
                    required = true
            )
            UUID carePlanId,

            @Parameter(description = "신청할 서비스 정보", required = true)
            @Valid
            CarePlanServiceSelectRequest carePlanServiceSelectRequest
    );

    @Operation(
            summary = "Care Plan 서비스 신청 취소",
            description = """
                    퇴원 예정자가 Care Plan에 신청한 서비스 항목을 논리 삭제한다.
                    
                    해당 서비스에 등록된 서비스 희망 일정이 존재하는 경우
                    함께 논리 삭제한다.
                    """
    )
    @ApiResponses
    ResponseEntity<Void> cancelCarePlanService(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "planServiceId",
                    description = "취소할 Care Plan 서비스 항목 ID",
                    required = true
            )
            UUID planServiceId
    );

    @Operation(
            summary = "Care Plan 신청 서비스 목록 조회",
            description = """
                    퇴원 예정자가 자신의 Care Plan에 신청한 서비스 목록을 조회한다.
                    
                    서비스 종류 정보와 함께 Care Plan에 등록된 서비스 항목을 반환한다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<PageResponse<CarePlanServiceSearchResponse>>> searchCarePlanServices(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "carePlanId",
                    description = "조회할 Care Plan ID",
                    required = true
            )
            UUID carePlanId,

            @Parameter(description = "페이지 번호")
            Integer page,

            @Parameter(description = "페이지 크기")
            Integer size
    );

    @Operation(
            summary = "Care Plan 신청 서비스 상세 조회",
            description = """
                    Care Plan에 신청된 특정 서비스 항목을 조회한다.
                    
                    carePlanId와 planServiceId를 기준으로 해당 Care Plan에 포함된
                    서비스 한 건을 조회하며 등록된 희망 일정도 함께 반환한다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<CarePlanServiceFindResponse>> findCarePlanService(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "carePlanId",
                    description = "Care Plan ID",
                    required = true
            )
            UUID carePlanId,

            @Parameter(
                    name = "planServiceId",
                    description = "조회할 Care Plan 서비스 항목 ID",
                    required = true
            )
            UUID planServiceId
    );
}
