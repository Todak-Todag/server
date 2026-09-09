package com.todak_todag.social_worker_service.matching.application.service.async;

import com.todak_todag.social_worker_service.matching.application.port.MatchableSocialWorkerPort;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingResultCommandService;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingQueryService;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStore;
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

    private final MatchableSocialWorkerPort matchableSocialWorkerPort;
    private final MatchingQueryService matchingQueryService;
    private final MatchingResultCommandService matchingResultCommandService;
    private final MatchingTaskStore matchingTaskStore;

    @Async("matchingTaskExecutor")
    public void process(
            UUID taskId,
            UUID matchingResultId,
            UUID patientId
    ) {

        MatchingTask task =
                matchingTaskStore
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
                    matchableSocialWorkerPort
                            .findMatchableSocialWorkerIds(
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
                    matchingQueryService.select(
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

    private void failMatching(
            UUID taskId,
            UUID matchingResultId
    ) {

        try {
            matchingResultCommandService.fail(
                    matchingResultId
            );
        } finally {

            MatchingTask task =
                    matchingTaskStore
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