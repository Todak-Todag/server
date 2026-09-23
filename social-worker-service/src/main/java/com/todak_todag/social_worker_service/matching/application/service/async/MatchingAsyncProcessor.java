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
                                        taskId,
                                        patientId
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
                        "[SocialWorkerMatching] 매칭 가능한 사회복지사 없음 "
                                + "taskId={}, matchingResultId={}, patientId={}",
                        taskId,
                        matchingResultId,
                        patientId
                );

                completeWithMatchingFailure(
                        taskId,
                        matchingResultId,
                        patientId
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

            completeTask(
                    taskId,
                    matchingResultId,
                    patientId
            );

            log.info(
                    "[SocialWorkerMatching] 자동 매칭 성공 "
                            + "taskId={}, matchingResultId={}, "
                            + "patientId={}, socialWorkerId={}",
                    taskId,
                    matchingResultId,
                    patientId,
                    selectedSocialWorkerId
            );

        } catch (Exception e) {

            log.error(
                    "[SocialWorkerMatching] 비동기 매칭 처리 실패 "
                            + "taskId={}, matchingResultId={}, patientId={}",
                    taskId,
                    matchingResultId,
                    patientId,
                    e
            );

            failTask(
                    taskId,
                    matchingResultId,
                    patientId
            );
        }
    }

    private void completeWithMatchingFailure(
            UUID taskId,
            UUID matchingResultId,
            UUID patientId
    ) {

        matchingResultCommandService.fail(
                matchingResultId
        );

        completeTask(
                taskId,
                matchingResultId,
                patientId
        );
    }

    private void completeTask(
            UUID taskId,
            UUID matchingResultId,
            UUID patientId
    ) {

        MatchingTask task =
                matchingTaskStore
                        .findByTaskId(taskId)
                        .orElseGet(
                                () -> MatchingTask.pending(
                                        taskId,
                                        patientId
                                )
                        );

        matchingTaskStore.save(
                task.completed(
                        matchingResultId
                )
        );
    }

    private void failTask(
            UUID taskId,
            UUID matchingResultId,
            UUID patientId
    ) {

        try {
            matchingResultCommandService.fail(
                    matchingResultId
            );

        } catch (Exception failException) {

            log.error(
                    "[SocialWorkerMatching] 매칭 결과 FAILED 변경 실패 "
                            + "taskId={}, matchingResultId={}, patientId={}",
                    taskId,
                    matchingResultId,
                    patientId,
                    failException
            );

        } finally {

            MatchingTask task =
                    matchingTaskStore
                            .findByTaskId(taskId)
                            .orElseGet(
                                    () -> MatchingTask.pending(
                                            taskId,
                                            patientId
                                    )
                            );

            matchingTaskStore.save(
                    task.failed()
            );
        }
    }
}