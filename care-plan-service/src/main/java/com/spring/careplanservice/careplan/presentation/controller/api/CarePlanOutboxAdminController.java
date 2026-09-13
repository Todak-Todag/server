package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.application.result.CarePlanOutboxEventAdminResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanOutboxCommandService;
import com.spring.careplanservice.careplan.application.service.query.CarePlanOutboxQueryService;
import com.spring.careplanservice.careplan.presentation.response.CarePlanOutboxEventAdminResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Outbox FAILED 이벤트 운영 확인/재처리용 관리자 API.
// 일반 사용자 대상 API가 아니므로 다른 컨트롤러와 달리 Swagger용 ApiSpec 인터페이스는 두지 않았다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/care-plan-outbox-events")
public class CarePlanOutboxAdminController {
    private final CarePlanOutboxQueryService carePlanOutboxQueryService;
    private final CarePlanOutboxCommandService carePlanOutboxCommandService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER')")
    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<List<CarePlanOutboxEventAdminResponse>>> findFailedOutboxEvents(
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        List<CarePlanOutboxEventAdminResult> results = carePlanOutboxQueryService.findFailed(limit);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "FAILED Outbox 이벤트 목록 조회 성공",
                        results.stream()
                                .map(CarePlanOutboxEventAdminResponse::from)
                                .toList()
                )
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER')")
    @PostMapping("/{outboxEventId}/retry")
    public ResponseEntity<Void> retryFailedOutboxEvent(
            @PathVariable UUID outboxEventId
    ) {
        carePlanOutboxCommandService.retryFailed(outboxEventId);

        return ResponseEntity.noContent().build();
    }
}
