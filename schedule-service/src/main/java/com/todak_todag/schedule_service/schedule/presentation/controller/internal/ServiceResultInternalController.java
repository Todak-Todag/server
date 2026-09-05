package com.todak_todag.schedule_service.schedule.presentation.controller.internal;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.schedule.application.result.InternalServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.application.service.query.InternalServiceResultQueryService;
import com.todak_todag.schedule_service.schedule.presentation.response.InternalServiceResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// X-Internal-Api-Key 검증은 InternalApiKeyInterceptor(global/security)가 수행 — Controller는 헤더를 직접 처리하지 않음
@RestController
@RequestMapping("/internal/v1/service-results")
@RequiredArgsConstructor
public class ServiceResultInternalController implements ServiceResultInternalApiSpec {

    private final InternalServiceResultQueryService internalServiceResultQueryService;

    // [내부 API] 서비스 수행 결과 조회
    @Override
    @GetMapping("/{serviceResultId}")
    public ResponseEntity<ApiResponse<InternalServiceResultResponse>> detail(@PathVariable UUID serviceResultId) {
        InternalServiceResultDetailResult result = internalServiceResultQueryService.findById(serviceResultId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 수행 결과 조회 성공",
                                InternalServiceResultResponse.from(result)
                        )
                );
    }
}
