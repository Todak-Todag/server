package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.presentation.request.ConsentCreateRequest;
import com.todak_todag.user_service.user.presentation.response.ConsentCreateResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentFindHistoryResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentWithdrawResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

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

    @Operation(
            summary = "로그인 사용자 약관 동의 내역 조회",
            description =
                    "로그인 사용자가 지금까지 동의하거나 철회한 약관 내역을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 사용자 동의 내역 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<ConsentFindHistoryResponse>>
    findMyConsents(
            @Parameter(hidden = true)
            UserContext user
    );

    @Operation(
            summary = "약관 동의 철회",
            description =
                    "로그인 사용자가 자신이 동의한 약관을 철회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약관 동의 철회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 동의 내역 ID"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인의 동의 내역이 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "동의 내역을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 철회된 동의 내역"
            )
    })
    ResponseEntity<ApiResponse<ConsentWithdrawResponse>>
    withdrawConsent(
            @Parameter(hidden = true)
            UserContext user,
            UUID consentId
    );
}