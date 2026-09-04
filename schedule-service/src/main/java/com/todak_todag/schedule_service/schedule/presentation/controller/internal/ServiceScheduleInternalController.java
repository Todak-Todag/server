package com.todak_todag.schedule_service.schedule.presentation.controller.internal;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.schedule.application.query.InternalServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.service.query.InternalServiceScheduleQueryService;
import com.todak_todag.schedule_service.schedule.application.result.InternalServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.presentation.response.InternalServiceScheduleListResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// X-Internal-Api-Key 검증은 InternalApiKeyInterceptor(global/security)가 수행 — Controller는 헤더를 직접 처리하지 않음
@RestController
@RequestMapping("/internal/v1/service-schedules")
@RequiredArgsConstructor
@Validated
public class ServiceScheduleInternalController implements ScheduleInternalApiSpec {

    private final InternalServiceScheduleQueryService internalServiceScheduleQueryService;

    // [내부 API] 서비스 제공자 일정 조회
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<InternalServiceScheduleListResponse>> search(
            // 1개 이상 검증
            @RequestParam @NotEmpty List<UUID> serviceOfferingIds,
            // yyyy-MM-dd 형식 일치
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        InternalServiceScheduleSearchQuery serviceScheduleSearchQuery = new InternalServiceScheduleSearchQuery(serviceOfferingIds, startDate);
        List<InternalServiceScheduleSearchResult> results = internalServiceScheduleQueryService.search(serviceScheduleSearchQuery);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 제공자 일정 조회 성공",
                                InternalServiceScheduleListResponse.of(results)
                        )
                );
    }
}
