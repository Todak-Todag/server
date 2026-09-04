package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.result.ConsentDocumentFindResult;
import com.todak_todag.user_service.user.application.service.query.ConsentDocumentQueryService;
import com.todak_todag.user_service.user.presentation.response.ConsentDocumentFindListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConsentDocumentController implements ConsentDocumentApiSpec {

    private final ConsentDocumentQueryService consentDocumentQueryService;

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
}