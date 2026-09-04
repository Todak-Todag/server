package com.spring.careplanservice.careplan.infrastructure.client;

import com.spring.careplanservice.global.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;


@FeignClient(
        name = "user-service",
        configuration = FeignConfig.class
)
public interface UserFeignClient {
    @GetMapping("/internal/v1/users/{userId}")
    UserInternalResponse findById(
            @PathVariable("userId") UUID userId
    );
}