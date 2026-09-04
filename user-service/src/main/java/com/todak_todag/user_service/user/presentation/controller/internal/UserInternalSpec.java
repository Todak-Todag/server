package com.todak_todag.user_service.user.presentation.controller.internal;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.presentation.response.UserInternalReadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Service Internal User", description = "내부 유저 API")
public interface UserInternalSpec {

    @Operation(
            summary = "로그인",
            description = """
                    내부 서비스가 호출하는 사용자 조회입니다.
                    
                    APPROVED 상태의 회원만 조회됩니다.
            """
    )
    ResponseEntity<ApiResponse<UserInternalReadResponse>> readUser(
            @Parameter(description = "조회할 사용자 ID", required = true)
            UUID userId
    );
}
