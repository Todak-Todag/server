package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.presentation.request.MatchingStatusChangeRequest;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingStatusChangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(
        name = "Social Worker Matching",
        description = "사회복지사 매칭 API"
)
public interface SocialWorkerMatchingStatusApiSpec {

    @Operation(
            summary = "사회복지사 매칭 상태 변경",
            description = """
                    사회복지사 매칭 결과의 상태를 변경한다.

                    요청 사용자와 역할을 기준으로 상태 변경 권한을 검증한다.
                    허용된 상태 전이인 경우에만 변경할 수 있다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사회복지사 매칭 상태 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 상태 변경 요청 또는 허용되지 않은 상태 전이"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "사회복지사 매칭 상태 변경 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매칭 결과를 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<MatchingStatusChangeResponse>> changeStatus(
            @Parameter(
                    name = "matchingResultId",
                    description = "상태를 변경할 사회복지사 매칭 결과 ID",
                    required = true
            )
            UUID matchingResultId,

            @Parameter(
                    description = "변경할 매칭 상태 정보",
                    required = true
            )
            @Valid
            MatchingStatusChangeRequest request,

            @Parameter(hidden = true)
            UserContext userContext
    );
}