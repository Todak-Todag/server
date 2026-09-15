package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingTaskStatusResponse;
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
public interface SocialWorkerMatchingTaskApiSpec {

    @Operation(
            summary = "사회복지사 매칭 처리 현황 조회",
            description = """
                    매칭 요청 시 발급된 taskId를 기준으로 비동기 처리 현황을 조회한다.

                    매칭을 요청한 환자만 해당 taskId의 처리 현황을 조회할 수 있다.
                    처리 상태를 통해 매칭 진행 여부를 확인할 수 있다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사회복지사 매칭 처리 현황 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "사회복지사 매칭 처리 현황 조회 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매칭 처리 작업을 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<MatchingTaskStatusResponse>> getMatchingTaskStatus(
            @Parameter(
                    name = "taskId",
                    description = "매칭 요청 처리 작업 ID",
                    required = true
            )
            UUID taskId,

            @Parameter(hidden = true)
            UserContext userContext
    );
}