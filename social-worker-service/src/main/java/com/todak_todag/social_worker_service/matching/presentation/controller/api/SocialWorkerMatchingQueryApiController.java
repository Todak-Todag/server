package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.query.MatchingResultQuery;
import com.todak_todag.social_worker_service.matching.application.result.MatchingResultQueryResult;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingQueryService;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingResultResponse;
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
public class SocialWorkerMatchingQueryApiController {

    private final MatchingQueryService matchingQueryService;

    @GetMapping("/{matchingResultId}")
    public ResponseEntity<ApiResponse<MatchingResultResponse>>
    getMatchingResult(
            @PathVariable UUID matchingResultId,
            @AuthenticationPrincipal UserContext userContext
    ) {

        validateAuthentication(
                userContext
        );

        MatchingResultQueryResult result =
                matchingQueryService
                        .getResult(
                                new MatchingResultQuery(
                                        matchingResultId,
                                        userContext.getUserId(),
                                        userContext.getRole()
                                )
                        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "사회복지사 매칭 결과 조회 성공",
                        MatchingResultResponse.from(
                                result
                        )
                )
        );
    }

    private void validateAuthentication(
            UserContext userContext
    ) {

        if (userContext == null) {

            throw new BusinessException(
                    MatchingErrorCode.MATCHING_QUERY_FORBIDDEN,
                    "사회복지사 매칭 결과 조회 실패"
            );
        }
    }
}