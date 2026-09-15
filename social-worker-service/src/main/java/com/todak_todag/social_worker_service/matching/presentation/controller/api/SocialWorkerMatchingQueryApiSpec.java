package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(
        name = "Social Worker Matching",
        description = "사회복지사 매칭 API"
)
public interface SocialWorkerMatchingQueryApiSpec {

    @Operation(
            summary = "사회복지사 매칭 결과 조회",
            description = """
                    매칭 결과 ID를 기준으로 사회복지사 매칭 결과를 조회한다.

                    요청 사용자의 ID와 역할을 기준으로 조회 권한을 검증한다.
                    매칭 상태와 배정된 사회복지사 정보를 확인할 수 있다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사회복지사 매칭 결과 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "사회복지사 매칭 결과 조회 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매칭 결과를 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<MatchingResultResponse>> getMatchingResult(
            @Parameter(
                    name = "matchingResultId",
                    description = "조회할 사회복지사 매칭 결과 ID",
                    required = true
            )
            UUID matchingResultId,

            @Parameter(hidden = true)
            UserContext userContext
    );
}