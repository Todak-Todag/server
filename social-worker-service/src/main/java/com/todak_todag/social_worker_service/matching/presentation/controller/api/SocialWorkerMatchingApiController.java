package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.common.UserRole;
import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingRequestCommandService;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/social-worker-matchings")
public class SocialWorkerMatchingApiController {

    private final MatchingRequestCommandService matchingRequestCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<MatchingRequestResponse>>
    requestMatching(
            @AuthenticationPrincipal UserContext userContext
    ) {

        validatePatient(
                userContext
        );

        UUID taskId =
                matchingRequestCommandService
                        .request(
                                userContext.getUserId()
                        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        ApiResponse.accepted(
                                "사회복지사 매칭 요청 접수 성공",
                                MatchingRequestResponse.from(
                                        taskId
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
                    MatchingErrorCode.MATCHING_FORBIDDEN,
                    "사회복지사 매칭 요청 실패"
            );
        }
    }
}