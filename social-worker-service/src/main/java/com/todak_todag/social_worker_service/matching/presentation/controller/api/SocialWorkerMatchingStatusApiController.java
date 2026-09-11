package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.command.MatchingStatusChangeCommand;
import com.todak_todag.social_worker_service.matching.application.result.MatchingStatusChangeResult;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingStatusCommandService;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.request.MatchingStatusChangeRequest;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingStatusChangeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/social-worker-matchings")
public class SocialWorkerMatchingStatusApiController {

    private final MatchingStatusCommandService matchingStatusCommandService;

    @PatchMapping("/{matchingResultId}")
    public ResponseEntity<ApiResponse<MatchingStatusChangeResponse>>
    changeStatus(
            @PathVariable UUID matchingResultId,
            @Valid @RequestBody MatchingStatusChangeRequest request,
            @AuthenticationPrincipal UserContext userContext
    ) {

        validateAuthentication(
                userContext
        );

        MatchingStatusChangeResult result =
                matchingStatusCommandService
                        .changeStatus(
                                new MatchingStatusChangeCommand(
                                        matchingResultId,
                                        request.status(),
                                        userContext.getUserId(),
                                        userContext.getRole()
                                )
                        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "사회복지사 매칭 상태 변경 성공",
                        MatchingStatusChangeResponse.from(
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
                    MatchingErrorCode.MATCHING_STATUS_CHANGE_FORBIDDEN,
                    "사회복지사 매칭 상태 변경 실패"
            );
        }
    }
}