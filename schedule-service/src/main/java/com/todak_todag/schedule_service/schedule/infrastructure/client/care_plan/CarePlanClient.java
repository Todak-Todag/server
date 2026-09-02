package com.todak_todag.schedule_service.schedule.infrastructure.client.care_plan;

import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanRangeInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.UUID;

// schedule-service -> care-plan-service Internal API
// 인증 헤더(X-Internal-Api-Key)는 요청마다 직접 붙이지 않고 FeignConfig의 RequestInterceptor가 전담
@FeignClient(name = "care-plan-service")
public interface CarePlanClient {

    @GetMapping("/internal/v1/service-preferences/{servicePreferenceId}/care-plan")
    ApiResponse<CarePlanRangeInternalResponse> findCarePlanRange(
            @PathVariable UUID servicePreferenceId
    );

}
