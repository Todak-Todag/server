package com.todak_todag.user_service.user.presentation.controller.internal;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/users")
public class UserInternalController implements UserInternalSpec {

    private UserQueryService userQueryService;

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> readUser(
            @PathVariable("userId") UUID userId
    ) {

        return null;
    }
}
