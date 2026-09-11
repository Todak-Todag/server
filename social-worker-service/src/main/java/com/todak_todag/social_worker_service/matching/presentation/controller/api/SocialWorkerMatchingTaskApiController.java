package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.common.UserRole;
import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.result.MatchingTaskStatusResult;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingTaskQueryService;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingTaskStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/social-worker-matchings")
public class SocialWorkerMatchingTaskApiController {

    private final MatchingTaskQueryService matchingTaskQueryService;

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<MatchingTaskStatusResponse>>
    getMatchingTaskStatus(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserContext userContext
    ) {

        validatePatient(
                userContext
        );

        MatchingTaskStatusResult result =
                matchingTaskQueryService
                        .getStatus(
                                taskId,
                                userContext.getUserId()
                        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "사회복지사 매칭 처리 현황 조회 성공",
                        MatchingTaskStatusResponse.from(
                                result
                        )
                )
        );
    }

    private void validatePatient(
            UserContext userContext
    ) {

        if (userContext == null
                || userContext.getRole() != UserRole.PATIENT) {

            throw new BusinessException(
                    MatchingErrorCode.MATCHING_TASK_FORBIDDEN,
                    "사회복지사 매칭 처리 현황 조회 실패"
            );
        }
    }
}