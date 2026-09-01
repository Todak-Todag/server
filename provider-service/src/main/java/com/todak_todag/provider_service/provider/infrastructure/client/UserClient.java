package com.todak_todag.provider_service.provider.infrastructure.client;

import com.todak_todag.provider_service.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/v1/users/{userId}")
    ApiResponse<UserInternalResponse> findById(@PathVariable("userId") UUID userId);

    record UserInternalResponse(
            UUID userId,
            String role,
            UUID regionId
    ) {
    }
}
