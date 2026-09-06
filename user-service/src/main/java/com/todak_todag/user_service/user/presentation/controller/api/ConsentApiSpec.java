package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.presentation.request.ConsentCreateRequest;
import com.todak_todag.user_service.user.presentation.response.ConsentCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "Consent",
        description = "사용자 약관 동의 API"
)
public interface ConsentApiSpec {

    @Operation(
            summary = "약관 동의",
            description =
                    "로그인한 사용자가 현재 적용 중인 하나 이상의 약관 버전에 동의한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "약관 동의 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description =
                            "요청 값 오류 또는 중복된 약관 버전"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description =
                            "동의할 수 없는 약관 버전 포함"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description =
                            "이미 동의한 약관 포함"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<ConsentCreateResponse>>
    createConsent(
            @Parameter(hidden = true)
            UserContext user,

            ConsentCreateRequest request
    );
}