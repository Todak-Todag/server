package com.todak_todag.social_worker_service.matching.application.service.async;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingResultCommandService;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingSelectionService;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserMatchableSocialWorkersResponse;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserServiceClient;
import com.todak_todag.social_worker_service.matching.infrastructure.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.infrastructure.task.MatchingTaskStore;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingAsyncProcessor {

    private static final int MAX_ATTEMPTS = 2;

    private final UserServiceClient userServiceClient;
    private final MatchingSelectionService matchingSelectionService;
    private final MatchingResultCommandService matchingResultCommandService;
    private final MatchingTaskStore matchingTaskStore;

    @Async("matchingTaskExecutor")
    public void process(
            UUID taskId,
            UUID matchingResultId,
            UUID patientId
    ) {

        MatchingTask task = matchingTaskStore
                .findByTaskId(taskId)
                .orElseGet(
                        () -> MatchingTask.pending(
                                taskId
                        )
                );

        matchingTaskStore.save(
                task.processing()
        );

        try {
            Set<UUID> candidateIds =
                    getCandidateIdsWithRetry(
                            patientId
                    );

            if (candidateIds == null
                    || candidateIds.isEmpty()) {

                log.info(
                        "[SocialWorkerMatching] 매칭 가능한 사회복지사 없음 patientId={}",
                        patientId
                );

                failMatching(
                        taskId,
                        matchingResultId
                );

                return;
            }

            UUID selectedSocialWorkerId =
                    matchingSelectionService.select(
                            candidateIds
                    );

            matchingResultCommandService.activate(
                    matchingResultId,
                    selectedSocialWorkerId
            );

            matchingTaskStore.save(
                    task.completed(
                            matchingResultId
                    )
            );

            log.info(
                    "[SocialWorkerMatching] 자동 매칭 성공 taskId={}, matchingResultId={}, patientId={}, socialWorkerId={}",
                    taskId,
                    matchingResultId,
                    patientId,
                    selectedSocialWorkerId
            );

        } catch (Exception e) {

            log.error(
                    "[SocialWorkerMatching] 자동 매칭 실패 taskId={}, matchingResultId={}, patientId={}",
                    taskId,
                    matchingResultId,
                    patientId,
                    e
            );

            failMatching(
                    taskId,
                    matchingResultId
            );
        }
    }

    private Set<UUID> getCandidateIdsWithRetry(
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
                        || response.data() == null) {
                    return Set.of();
                }

                Set<UUID> socialWorkerIds =
                        response.data()
                                .socialWorkerIds();

                return socialWorkerIds != null
                        ? socialWorkerIds
                        : Set.of();

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

    private void failMatching(
            UUID taskId,
            UUID matchingResultId
    ) {

        try {
            matchingResultCommandService.fail(
                    matchingResultId
            );
        } finally {

            MatchingTask task = matchingTaskStore
                    .findByTaskId(taskId)
                    .orElseGet(
                            () -> MatchingTask.pending(
                                    taskId
                            )
                    );

            matchingTaskStore.save(
                    task.failed(
                            matchingResultId
                    )
            );
        }
    }
}