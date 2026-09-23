package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "Social Worker Matching",
        description = "사회복지사 매칭 API"
)
public interface SocialWorkerMatchingApiSpec {

    @Operation(
            summary = "사회복지사 매칭 요청",
            description = """
                    퇴원 예정자가 사회복지사 자동 매칭을 요청한다.

                    요청이 접수되면 비동기 매칭 처리가 시작되며,
                    처리 현황을 조회할 수 있는 taskId를 반환한다.
                    환자(PATIENT)만 요청할 수 있다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "사회복지사 매칭 요청 접수 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "사회복지사 매칭 요청 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 진행 중인 매칭이 존재함"
            )
    })
    ResponseEntity<ApiResponse<MatchingRequestResponse>> requestMatching(
            @Parameter(hidden = true)
            UserContext userContext
    );
}