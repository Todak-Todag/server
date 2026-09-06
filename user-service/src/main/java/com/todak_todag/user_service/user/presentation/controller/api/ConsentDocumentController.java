package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.user.application.result.ConsentDocumentCreateResult;
import com.todak_todag.user_service.user.presentation.request.ConsentDocumentCreateRequest;
import com.todak_todag.user_service.user.presentation.response.ConsentDocumentCreateResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.ConsentDocumentDeleteCommand;
import com.todak_todag.user_service.user.application.result.ConsentDocumentFindDetailResult;
import com.todak_todag.user_service.user.application.result.ConsentDocumentFindResult;
import com.todak_todag.user_service.user.application.result.ConsentDocumentUpdateRequiredResult;
import com.todak_todag.user_service.user.application.service.command.ConsentDocumentCommandService;
import com.todak_todag.user_service.user.application.service.query.ConsentDocumentQueryService;
import com.todak_todag.user_service.user.presentation.request.ConsentDocumentUpdateRequiredRequest;
import com.todak_todag.user_service.user.presentation.response.ConsentDocumentFindDetailResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentDocumentFindListResponse;
import com.todak_todag.user_service.user.presentation.response.ConsentDocumentUpdateRequiredResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConsentDocumentController implements ConsentDocumentApiSpec {

    private final ConsentDocumentQueryService consentDocumentQueryService;
    private final ConsentDocumentCommandService consentDocumentCommandService;

    // 현재 적용 중인 약관 목록 조회
    @Override
    @GetMapping("/consent-documents")
    public ResponseEntity<ApiResponse<ConsentDocumentFindListResponse>>
    findCurrentConsentDocuments() {

        List<ConsentDocumentFindResult> results =
                consentDocumentQueryService.findCurrentConsentDocuments();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "현재 적용 중인 약관 목록 조회 성공",
                                ConsentDocumentFindListResponse.of(results)
                        )
                );
    }

    // 약관 버전 상세 조회
    @Override
    @GetMapping("/consent-documents/{consentDocumentVersionId}")
    public ResponseEntity<ApiResponse<ConsentDocumentFindDetailResponse>>
    findConsentDocumentDetail(
            @PathVariable UUID consentDocumentVersionId
    ) {
        ConsentDocumentFindDetailResult result =
                consentDocumentQueryService.findConsentDocumentDetail(
                        consentDocumentVersionId
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "약관 상세 조회 성공",
                                ConsentDocumentFindDetailResponse.from(result)
                        )
                );
    }

    // 약관 필수/선택 여부 변경
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @PatchMapping(
            "/admin/consent-documents/{consentDocumentId}/required"
    )
    public ResponseEntity<ApiResponse<ConsentDocumentUpdateRequiredResponse>>
    updateConsentDocumentRequired(
            @PathVariable UUID consentDocumentId,
            @Valid @RequestBody ConsentDocumentUpdateRequiredRequest request
    ) {
        ConsentDocumentUpdateRequiredResult result =
                consentDocumentCommandService.updateRequired(
                        request.toCommand(consentDocumentId)
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "약관 필수 여부 변경 성공",
                                ConsentDocumentUpdateRequiredResponse.from(result)
                        )
                );
    }

    // 약관 사용 종료 (논리 삭제)
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping(
            "/admin/consent-documents/{consentDocumentId}"
    )
    public ResponseEntity<Void> deleteConsentDocument(
            @PathVariable UUID consentDocumentId,
            @AuthenticationPrincipal UserContext user
    ) {
        consentDocumentCommandService.delete(
                new ConsentDocumentDeleteCommand(
                        consentDocumentId,
                        user.getUserId()
                )
        );

        return ResponseEntity.noContent().build();
    }

    // 신규 약관 및 최초 버전 등록
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @PostMapping("/admin/consent-documents")
    public ResponseEntity<ApiResponse<ConsentDocumentCreateResponse>>
    createConsentDocument(
            @Valid @RequestBody ConsentDocumentCreateRequest request
    ) {

        ConsentDocumentCreateResult result =
                consentDocumentCommandService.create(
                        request.toCommand()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "약관 등록 성공",
                                ConsentDocumentCreateResponse.from(result)
                        )
                );
    }
}