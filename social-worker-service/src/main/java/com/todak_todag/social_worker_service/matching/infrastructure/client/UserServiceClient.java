package com.todak_todag.social_worker_service.matching.infrastructure.client;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping(
            "/internal/v1/users/{patientId}/social-worker-match"
    )
    ApiResponse<UserMatchableSocialWorkersResponse>
    getMatchableSocialWorkers(
            @PathVariable("patientId") UUID patientId
    );
}