package com.todak_todag.social_worker_service.matching.infrastructure.adapter;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.matching.application.port.MatchableSocialWorkerPort;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserMatchableSocialWorkersResponse;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserServiceClient;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchableSocialWorkerAdapter
        implements MatchableSocialWorkerPort {

    private static final int MAX_ATTEMPTS = 2;

    private final UserServiceClient userServiceClient;

    @Override
    public Set<UUID> findMatchableSocialWorkerIds(
            UUID patientId
    ) {

        RuntimeException lastException = null;

        for (int attempt = 1;
             attempt <= MAX_ATTEMPTS;
             attempt++) {

            try {
                ApiResponse<UserMatchableSocialWorkersResponse> response =
                        userServiceClient
                                .getMatchableSocialWorkers(
                                        patientId
                                );

                if (response == null
                        || response.data() == null
                        || response.data().socialWorkerIds() == null) {

                    return Set.of();
                }

                return response.data()
                        .socialWorkerIds();

            } catch (RetryableException e) {

                lastException = e;

                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }

                log.warn(
                        "[SocialWorkerMatching] User-Service 호출 재시도 attempt={}, patientId={}",
                        attempt,
                        patientId
                );

            } catch (FeignException e) {

                lastException = e;

                boolean retryableStatus =
                        e.status() >= 500;

                if (!retryableStatus
                        || attempt == MAX_ATTEMPTS) {

                    throw e;
                }

                log.warn(
                        "[SocialWorkerMatching] User-Service 5xx 재시도 attempt={}, status={}, patientId={}",
                        attempt,
                        e.status(),
                        patientId
                );
            }
        }

        throw lastException != null
                ? lastException
                : new IllegalStateException(
                "User-Service 호출에 실패했습니다."
        );
    }
}