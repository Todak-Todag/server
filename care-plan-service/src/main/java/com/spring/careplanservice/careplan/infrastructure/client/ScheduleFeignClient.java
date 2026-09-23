package com.spring.careplanservice.careplan.infrastructure.client;

import com.spring.careplanservice.global.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "schedule-service",
        configuration = FeignConfig.class
)
public interface ScheduleFeignClient {
    @GetMapping("/internal/v1/service-results/{serviceResultId}")
    ScheduleInternalResponse findServiceResult(
            @PathVariable("serviceResultId") UUID serviceResultId
    );
}
