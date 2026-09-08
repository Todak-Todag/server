package com.todak_todag.social_worker_service.matching.application.service.async;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingResultCommandService;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingSelectionService;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserMatchableSocialWorkersResponse;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserServiceClient;
import com.todak_todag.social_worker_service.matching.infrastructure.task.InMemoryMatchingTaskStore;
import com.todak_todag.social_worker_service.matching.infrastructure.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.infrastructure.task.MatchingTaskStatus;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MatchingAsyncProcessorTest {

    private UserServiceClient userServiceClient;
    private MatchingSelectionService matchingSelectionService;
    private MatchingResultCommandService matchingResultCommandService;

    private InMemoryMatchingTaskStore matchingTaskStore;
    private MatchingAsyncProcessor processor;

    @BeforeEach
    void setUp() {

        userServiceClient =
                mock(UserServiceClient.class);

        matchingSelectionService =
                mock(MatchingSelectionService.class);

        matchingResultCommandService =
                mock(MatchingResultCommandService.class);

        matchingTaskStore =
                new InMemoryMatchingTaskStore();

        processor =
                new MatchingAsyncProcessor(
                        userServiceClient,
                        matchingSelectionService,
                        matchingResultCommandService,
                        matchingTaskStore
                );
    }

    @Test
    @DisplayName("사회복지사 후보가 없으면 재시도하지 않고 FAILED 처리한다")
    void emptyCandidatesFailWithoutRetry() {

        UUID taskId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask.pending(taskId)
        );

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenReturn(
                ApiResponse.ok(
                        "사회복지사 정보 조회 성공",
                        new UserMatchableSocialWorkersResponse(
                                Set.of()
                        )
                )
        );

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                userServiceClient,
                times(1)
        ).getMatchableSocialWorkers(patientId);

        verify(
                matchingResultCommandService,
                times(1)
        ).fail(resultId);

        verifyNoInteractions(
                matchingSelectionService
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
    @DisplayName("User-Service 4xx 오류는 재시도하지 않고 FAILED 처리한다")
    void clientErrorDoesNotRetry() {

        UUID taskId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask.pending(taskId)
        );

        FeignException clientError =
                mock(FeignException.class);

        when(clientError.status())
                .thenReturn(404);

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenThrow(clientError);

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                userServiceClient,
                times(1)
        ).getMatchableSocialWorkers(patientId);

        verify(
                matchingResultCommandService,
                times(1)
        ).fail(resultId);

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
    @DisplayName("User-Service 5xx 오류 후 재시도 성공하면 매칭을 완료한다")
    void serverErrorRetriesOnceAndSucceeds() {

        UUID taskId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask.pending(taskId)
        );

        FeignException serverError =
                mock(FeignException.class);

        when(serverError.status())
                .thenReturn(500);

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        )
                .thenThrow(serverError)
                .thenReturn(
                        ApiResponse.ok(
                                "사회복지사 정보 조회 성공",
                                new UserMatchableSocialWorkersResponse(
                                        Set.of(workerId)
                                )
                        )
                );

        when(
                matchingSelectionService
                        .select(Set.of(workerId))
        ).thenReturn(workerId);

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                userServiceClient,
                times(2)
        ).getMatchableSocialWorkers(patientId);

        verify(
                matchingResultCommandService,
                times(1)
        ).activate(
                resultId,
                workerId
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

    @Test
    @DisplayName("User-Service 5xx 오류가 계속되면 1회 재시도 후 FAILED 처리한다")
    void repeatedServerErrorFailsAfterOneRetry() {

        UUID taskId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask.pending(taskId)
        );

        FeignException serverError =
                mock(FeignException.class);

        when(serverError.status())
                .thenReturn(500);

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenThrow(serverError);

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                userServiceClient,
                times(2)
        ).getMatchableSocialWorkers(patientId);

        verify(
                matchingResultCommandService,
                times(1)
        ).fail(resultId);

        verify(
                matchingResultCommandService,
                never()
        ).activate(
                any(),
                any()
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
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenReturn(
                ApiResponse.ok(
                        "사회복지사 정보 조회 성공",
                        new UserMatchableSocialWorkersResponse(
                                candidates
                        )
                )
        );

        when(
                matchingSelectionService
                        .select(candidates)
        ).thenReturn(workerA);

        processor.process(
                taskId,
                resultId,
                patientId
        );

        verify(
                matchingSelectionService,
                times(1)
        ).select(candidates);

        verify(
                matchingResultCommandService,
                times(1)
        ).activate(
                resultId,
                workerA
        );

        MatchingTask task =
                matchingTaskStore
                        .findByTaskId(taskId)
                        .orElseThrow();

        assertEquals(
                MatchingTaskStatus.COMPLETED,
                task.status()
        );
    }
}