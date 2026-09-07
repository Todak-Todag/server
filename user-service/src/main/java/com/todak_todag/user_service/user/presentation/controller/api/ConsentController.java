package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.result.ConsentCreateResult;
import com.todak_todag.user_service.user.application.service.command.ConsentCommandService;
import com.todak_todag.user_service.user.presentation.request.ConsentCreateRequest;
import com.todak_todag.user_service.user.presentation.response.ConsentCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConsentController
        implements ConsentApiSpec {

    private final ConsentCommandService
            consentCommandService;

    //
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
}