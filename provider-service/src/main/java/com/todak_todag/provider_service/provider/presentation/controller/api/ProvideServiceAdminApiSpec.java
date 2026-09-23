package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.provider.presentation.request.ProvideServiceCreateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Provide Service - Admin", description = "관리자 서비스 종류 API")
public interface ProvideServiceAdminApiSpec {

    @Operation(
            summary = "서비스 종류 등록",
            description = """
                    전 지역 공통으로 사용할 서비스 종류(방문요양, 방문간호 등)를 등록한다.

                    관리자(MASTER)만 등록할 수 있다.
                    이미 등록된 서비스명은 중복 등록할 수 없다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "서비스 종류 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "서비스명 또는 서비스 내용 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "MASTER 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 등록된 서비스명"
            )
    })
    ApiResponse<ProvideServiceCreateResponse> create(
            @Parameter(description = "서비스 종류 등록 정보", required = true)
            @Valid
            ProvideServiceCreateRequest request
    );
}
