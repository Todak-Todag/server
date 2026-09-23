package com.spring.careplanservice.careplan.infrastructure.client;

import com.spring.careplanservice.global.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "provider-service",
        configuration = FeignConfig.class
)
public interface ProviderServiceFeignClient {
    @GetMapping("/internal/v1/provide-services")
    ProviderServiceInternalResponse search(
            @RequestParam("provideServiceIds") List<UUID> provideServiceIds
    );
}
