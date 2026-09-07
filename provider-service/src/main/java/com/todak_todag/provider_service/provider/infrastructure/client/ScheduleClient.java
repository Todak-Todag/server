package com.todak_todag.provider_service.provider.infrastructure.client;

import com.todak_todag.provider_service.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "schedule-service")
public interface ScheduleClient {

    @GetMapping("/internal/v1/service-schedules")
    ApiResponse<ServiceScheduleListResponse> findSchedules(
            @RequestParam("serviceOfferingIds") List<UUID> serviceOfferingIds,
            // Feign이 LocalDate를 로케일 기본 포맷("26. 9. 10.")으로 직렬화해 ISO 형식을 명시한다
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    );

    record ServiceScheduleListResponse(
            List<ServiceScheduleResponse> content
    ) {
    }

    record ServiceScheduleResponse(
            UUID serviceScheduleId,
            UUID serviceOfferingId,
            LocalDate date,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String status
    ) {
    }
}