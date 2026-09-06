package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.presentation.request.ConsentDocumentCreateRequest;
import com.todak_todag.user_service.user.presentation.request.ConsentDocumentUpdateRequiredRequest;
import com.todak_todag.user_service.user.presentation.request.ConsentDocumentVersionCreateRequest;
import com.todak_todag.user_service.user.presentation.response.*;
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

    @Operation(
            summary = "약관 필수 여부 변경",
            description = "기존 약관 문서의 필수 또는 선택 동의 여부를 변경한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약관 필수 여부 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request Validation 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "약관 필수 여부 변경 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약관 문서를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<ConsentDocumentUpdateRequiredResponse>>
    updateConsentDocumentRequired(
            @Parameter(
                    description = "약관 문서 ID",
                    required = true
            )
            UUID consentDocumentId,
            ConsentDocumentUpdateRequiredRequest request
    );

    @Operation(
            summary = "약관 사용 종료",
            description = "더 이상 서비스에서 사용하지 않는 약관 문서를 논리적으로 삭제한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "약관 사용 종료 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 약관 문서 ID 형식"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "약관 사용 종료 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약관 문서를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 사용 종료된 약관"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<Void> deleteConsentDocument(
            @Parameter(
                    description = "약관 문서 ID",
                    required = true
            )
            UUID consentDocumentId,

            @Parameter(hidden = true)
            UserContext user
    );

    @Operation(
            summary = "신규 약관 및 최초 버전 등록",
            description = "새로운 약관 문서와 최초 약관 버전을 하나의 트랜잭션으로 등록한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "약관 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request Validation 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "약관 등록 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "동일한 약관 유형이 이미 존재"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<ConsentDocumentCreateResponse>>
    createConsentDocument(
            ConsentDocumentCreateRequest request
    );

    @Operation(
            summary = "신규 약관 버전 등록",
            description = "기존 약관 문서에 변경된 내용의 새로운 약관 버전을 등록한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "약관 버전 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request Validation 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "약관 버전 등록 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약관 문서를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "동일한 약관 버전이 이미 존재"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<ConsentDocumentVersionCreateResponse>>
    createConsentDocumentVersion(
            @Parameter(
                    description = "약관 문서 ID",
                    required = true
            )
            UUID consentDocumentId,
            ConsentDocumentVersionCreateRequest request
    );
}