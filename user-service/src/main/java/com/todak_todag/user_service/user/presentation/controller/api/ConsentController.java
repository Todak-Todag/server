package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.ConsentWithdrawCommand;
import com.todak_todag.user_service.user.application.result.ConsentCreateResult;
import com.todak_todag.user_service.user.application.result.ConsentFindHistoryResult;
import com.todak_todag.user_service.user.application.result.ConsentWithdrawResult;
import com.todak_todag.user_service.user.application.service.command.ConsentCommandService;
import com.todak_todag.user_service.user.application.service.query.ConsentQueryService;
import com.todak_todag.user_service.user.presentation.request.ConsentCreateRequest;
import com.todak_todag.user_service.user.presentation.response.ConsentCreateResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentFindHistoryResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentWithdrawResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConsentController
        implements ConsentApiSpec {

    private final ConsentCommandService
            consentCommandService;
    private final ConsentQueryService
            consentQueryService;


    @Override
    @PostMapping("/consents")
    public ResponseEntity<ApiResponse<ConsentCreateResponse>>
    createConsent(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody ConsentCreateRequest request
    ) {

        ConsentCreateResult result =
                consentCommandService.create(
                        request.toCommand(
                                user.getUserId()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "약관 동의 성공",
                                ConsentCreateResponse.from(
                                        result
                                )
                        )
                );
    }

    // 로그인 사용자 약관 동의 내역 조회
    @Override
    @GetMapping("/consents/me")
    public ResponseEntity<ApiResponse<ConsentFindHistoryResponse>>
    findMyConsents(
            @AuthenticationPrincipal UserContext user
    ) {

        List<ConsentFindHistoryResult> results =
                consentQueryService.findMyConsents(
                        user.getUserId()
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "약관 동의 내역 조회 성공",
                                ConsentFindHistoryResponse.of(
                                        results
                                )
                        )
                );
    }

    // 로그인 사용자 약관 동의 철회
    @Override
    @PostMapping("/consents/{consentId}/withdraw")
    public ResponseEntity<ApiResponse<ConsentWithdrawResponse>>
    withdrawConsent(
            @AuthenticationPrincipal UserContext user,
            @PathVariable UUID consentId
    ) {
        ConsentWithdrawResult result =
                consentCommandService.withdraw(
                        new ConsentWithdrawCommand(
                                user.getUserId(),
                                consentId
                        )
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "약관 동의 철회 성공",
                                ConsentWithdrawResponse.from(
                                        result
                                )
                        )
                );
    }
}