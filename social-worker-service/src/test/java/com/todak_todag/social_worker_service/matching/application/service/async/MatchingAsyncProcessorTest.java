package com.todak_todag.social_worker_service.matching.application.service.async;

import com.todak_todag.social_worker_service.matching.application.port.MatchableSocialWorkerPort;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingResultCommandService;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingQueryService;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStatus;
import com.todak_todag.social_worker_service.matching.infrastructure.task.InMemoryMatchingTaskStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MatchingAsyncProcessorTest {

    private MatchableSocialWorkerPort matchableSocialWorkerPort;
    private MatchingQueryService matchingQueryService;
    private MatchingResultCommandService matchingResultCommandService;

    private InMemoryMatchingTaskStore matchingTaskStore;
    private MatchingAsyncProcessor processor;

    @BeforeEach
    void setUp() {

        matchableSocialWorkerPort =
                mock(MatchableSocialWorkerPort.class);

        matchingQueryService =
                mock(MatchingQueryService.class);

        matchingResultCommandService =
                mock(MatchingResultCommandService.class);

        matchingTaskStore =
                new InMemoryMatchingTaskStore();

        processor =
                new MatchingAsyncProcessor(
                        matchableSocialWorkerPort,
                        matchingQueryService,
                        matchingResultCommandService,
                        matchingTaskStore
                );
    }

    @Test
    @DisplayName("사회복지사 후보가 없으면 FAILED 처리한다")
    void emptyCandidatesFail() {

        UUID taskId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask.pending(taskId)
        );

        when(
                matchableSocialWorkerPort
                        .findMatchableSocialWorkerIds(
                                patientId
                        )
        ).thenReturn(
                Set.of()
        );

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                matchableSocialWorkerPort,
                times(1)
        ).findMatchableSocialWorkerIds(
                patientId
        );

        verify(
                matchingResultCommandService,
                times(1)
        ).fail(resultId);

        verifyNoInteractions(
                matchingQueryService
        );

        MatchingTask task =
                matchingTaskStore
                        .findByTaskId(taskId)
                        .orElseThrow();

        assertEquals(
                MatchingTaskStatus.FAILED,
                task.status()
        );

        assertEquals(
                resultId,
                task.matchingResultId()
        );
    }

    @Test
    @DisplayName("사회복지사 후보 조회 중 예외가 발생하면 FAILED 처리한다")
    void candidateLookupFailureFailsMatching() {

        UUID taskId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask.pending(taskId)
        );

        when(
                matchableSocialWorkerPort
                        .findMatchableSocialWorkerIds(
                                patientId
                        )
        ).thenThrow(
                new RuntimeException(
                        "User-Service 호출 실패"
                )
        );

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                matchingResultCommandService,
                times(1)
        ).fail(resultId);

        verifyNoInteractions(
                matchingQueryService
        );

        MatchingTask task =
                matchingTaskStore
                        .findByTaskId(taskId)
                        .orElseThrow();

        assertEquals(
                MatchingTaskStatus.FAILED,
                task.status()
        );
    }

    @Test
    @DisplayName("정상 후보가 존재하면 선택된 사회복지사로 ACTIVE 처리한다")
    void successfulMatchingActivatesResult() {

        UUID taskId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        UUID workerA =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                );

        UUID workerB =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000002"
                );

        Set<UUID> candidates =
                Set.of(
                        workerA,
                        workerB
                );

        matchingTaskStore.save(
                MatchingTask.pending(taskId)
        );

        when(
                matchableSocialWorkerPort
                        .findMatchableSocialWorkerIds(
                                patientId
                        )
        ).thenReturn(
                candidates
        );

        when(
                matchingQueryService
                        .select(candidates)
        ).thenReturn(workerA);

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                matchingQueryService,
                times(1)
        ).select(candidates);

        verify(
                matchingResultCommandService,
                times(1)
        ).activate(
                resultId,
                workerA
        );

        verify(
                matchingResultCommandService,
                never()
        ).fail(any());

        MatchingTask task =
                matchingTaskStore
                        .findByTaskId(taskId)
                        .orElseThrow();

        assertEquals(
                MatchingTaskStatus.COMPLETED,
                task.status()
        );

        assertEquals(
                resultId,
                task.matchingResultId()
        );
    }
}