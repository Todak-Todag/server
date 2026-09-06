package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentDocumentFindDetailResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentDocumentFindListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(
        name = "Consent Document",
        description = "약관 문서 API"
)
public interface ConsentDocumentApiSpec {

    @Operation(
            summary = "현재 적용 중인 약관 목록 조회",
            description = "현재 시점을 기준으로 적용 중인 최신 버전의 약관 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "현재 적용 중인 약관 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<ConsentDocumentFindListResponse>>
    findCurrentConsentDocuments();

    @Operation(
            summary = "약관 상세 조회",
            description = "약관 버전 ID를 기준으로 약관 버전의 상세 정보를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약관 상세 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 약관 버전 ID 형식"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 약관을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<ConsentDocumentFindDetailResponse>>
    findConsentDocumentDetail(
            @Parameter(
                    description = "약관 버전 ID",
                    required = true
            )
            UUID consentDocumentVersionId
    );
}