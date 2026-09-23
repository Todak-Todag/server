package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.presentation.request.MatchingAttemptRetryRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.MatchingAttemptRetryResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.MatchingAttemptSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Matching Attempt", description = "서비스 매칭 시도 API")
public interface MatchingAttemptApiSpec {

    @Operation(
            summary = "매칭 실패 내역 조회",
            description = "퇴원 예정자가 본인의 서비스 희망 일정에 대한 매칭 시도(성공/실패) 내역을 조회한다. " +
                    "status를 지정하지 않으면 재매칭이 필요한 FAILED 내역만 조회한다 — " +
                    "이때 아직 일정이 생성되지 않은(=재시도로 해소되지 않은) 실패 건만 반환한다. " +
                    "EXPIRED는 끝내 재매칭되지 않아 Care Plan 종료와 함께 만료 처리된 실패 이력이다. " +
                    "Care Plan이 CONFIRMED 상태가 아니면 빈 배열을 반환한다. " +
                    "정렬은 최신순/오래된순(기본 createdAt,DESC)이 가능하다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매칭 실패 내역 조회 성공 (없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "status에 MATCHED/FAILED/EXPIRED 이외의 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PATIENT가 아니거나, 요청자의 Care Plan이 없거나 아직 UNDER_REVIEW"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "care-plan-service가 요청을 거부"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "care-plan-service 응답을 처리할 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "care-plan-service 호출 실패 (연결 불가 또는 상대 서버 오류)")
    })
    ResponseEntity<ApiResponse<PageResponse<MatchingAttemptSearchResponse>>> search(
            @Parameter(description = "페이지 번호 (기본 0)")
            Integer page,
            @Parameter(description = "페이지 크기 (10/30/50, 이외 값은 10으로 자동 보정, 기본 10)")
            Integer size,
            @Parameter(description = "정렬 (기본 createdAt,DESC)")
            String sort,
            @Parameter(description = "매칭 결과 필터 (MATCHED/FAILED/EXPIRED, 기본 FAILED)")
            MatchingAttemptStatus status,
            @Parameter(hidden = true)
            UserContext user
    );

    @Operation(
            summary = "재매칭 시도",
            description = "퇴원 예정자가 매칭에 실패(FAILED)한 서비스 희망 일정에 대해 새로운 희망 날짜/시간대로 재매칭을 요청한다. " +
                    "요청이 접수되면 ProviderReMatched 이벤트만 발행되고(202), 새로운 매칭 시도 이력은 " +
                    "Provider-Service의 매칭 결과 이벤트를 수신할 때 추가된다. 결과는 매칭 실패 내역 조회 API로 확인한다. " +
                    "대상이 FAILED 상태가 아니거나 이미 재시도가 접수된 경우 409를 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "재매칭 시도 접수 성공 (결과는 매칭 실패 내역 조회 API로 확인)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "재매칭 희망 날짜가 Care Plan 일정 범위를 벗어남, 또는 date 누락/preferredTimeSlot 허용되지 않는 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아니거나, 존재하지 않는 매칭 시도 (리소스 존재 여부 비노출)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "매칭 시도가 FAILED 상태가 아니거나, 이미 재시도가 접수됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "care-plan-service가 요청을 거부"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "care-plan-service 응답을 처리할 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "care-plan-service 호출 실패 (연결 불가 또는 상대 서버 오류)")
    })
    ResponseEntity<ApiResponse<MatchingAttemptRetryResponse>> retry(
            @Parameter(name = "matchingAttemptId", description = "재시도할 매칭 시도 ID", required = true)
            UUID matchingAttemptId,
            @Valid
            MatchingAttemptRetryRequest request,
            @Parameter(hidden = true)
            UserContext user
    );
}
