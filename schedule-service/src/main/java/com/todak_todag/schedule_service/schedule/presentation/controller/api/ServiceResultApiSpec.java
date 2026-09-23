package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.presentation.request.ServiceResultRegisterRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.ServiceResultDetailResponse;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "서비스 수행 결과 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "startedAt/finishedAt 누락·형식 오류, 또는 finishedAt이 startedAt보다 이르거나 같음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 배정된 서비스 제공자가 아니거나, 존재하지 않는 서비스 일정 (리소스 존재 여부 비노출)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "일정의 status가 COMPLETED/NO_SHOW가 아니거나, 이미 결과가 등록된 서비스 일정"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "provider-service가 요청을 거부"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "provider-service 응답을 처리할 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "provider-service 호출 실패 (연결 불가 또는 상대 서버 오류)")
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서비스 수행 결과 목록 조회 성공 (없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "page/size 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PATIENT/SERVICE_PROVIDER가 아닌 역할로 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "care-plan-service 또는 provider-service가 요청을 거부"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "care-plan-service 또는 provider-service 응답을 처리할 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "care-plan-service 또는 provider-service 호출 실패 (연결 불가 또는 상대 서버 오류)")
    })
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

    @Operation(
            summary = "서비스 수행 결과 상세 조회",
            description = "퇴원 예정자는 본인이 받은, 서비스 제공자는 본인이 제공한 서비스 수행 결과 하나의 상세 정보를 조회한다. " +
                    "소유권은 해당 수행 결과가 속한 서비스 일정의 servicePreferenceId/serviceOfferingId를 기준으로 " +
                    "care-plan-service / provider-service Internal API를 호출해 검증한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서비스 수행 결과 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "serviceResultId가 UUID 형식이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아니거나, 존재하지 않는 수행 결과/일정 (리소스 존재 여부 비노출)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "care-plan-service 또는 provider-service가 요청을 거부"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "care-plan-service 또는 provider-service 응답을 처리할 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "care-plan-service 또는 provider-service 호출 실패 (연결 불가 또는 상대 서버 오류)")
    })
    ResponseEntity<ApiResponse<ServiceResultDetailResponse>> detail(
            @Parameter(name = "serviceResultId", description = "조회할 수행 결과 ID", required = true)
            UUID serviceResultId,
            @Parameter(hidden = true)
            UserContext user
    );
}
