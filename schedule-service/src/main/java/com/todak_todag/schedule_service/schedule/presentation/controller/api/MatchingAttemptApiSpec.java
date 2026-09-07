package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.presentation.response.MatchingAttemptSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Matching Attempt", description = "서비스 매칭 시도 API")
public interface MatchingAttemptApiSpec {

    @Operation(
            summary = "매칭 실패 내역 조회",
            description = "퇴원 예정자가 본인의 서비스 희망 일정에 대한 매칭 시도(성공/실패) 내역을 조회한다. " +
                    "status를 지정하지 않으면 재매칭이 필요한 FAILED 내역만 조회한다 — " +
                    "이때 아직 일정이 생성되지 않은(=재시도로 해소되지 않은) 실패 건만 반환한다. " +
                    "Care Plan이 CONFIRMED 상태가 아니면 빈 배열을 반환한다. " +
                    "정렬은 최신순/오래된순(기본 createdAt,DESC)이 가능하다."
    )
    @ApiResponses
    ResponseEntity<ApiResponse<PageResponse<MatchingAttemptSearchResponse>>> search(
            @Parameter(description = "페이지 번호 (기본 0)")
            Integer page,
            @Parameter(description = "페이지 크기 (10/30/50, 이외 값은 10으로 자동 보정, 기본 10)")
            Integer size,
            @Parameter(description = "정렬 (기본 createdAt,DESC)")
            String sort,
            @Parameter(description = "매칭 결과 필터 (MATCHED/FAILED, 기본 FAILED)")
            MatchingAttemptStatus status,
            @Parameter(hidden = true)
            UserContext user
    );
}
