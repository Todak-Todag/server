package com.todak_todag.social_worker_service.matching.application.service.query;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.result.MatchingTaskStatusResult;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStatus;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.infrastructure.task.InMemoryMatchingTaskStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchingTaskQueryServiceTest {

    private InMemoryMatchingTaskStore matchingTaskStore;
    private MatchingTaskQueryService service;

    @BeforeEach
    void setUp() {

        matchingTaskStore =
                new InMemoryMatchingTaskStore();

        service =
                new MatchingTaskQueryService(
                        matchingTaskStore
                );
    }

    @Test
    @DisplayName("본인의 COMPLETED Task 처리 현황을 조회할 수 있다")
    void getCompletedTaskStatus() {

        UUID taskId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID matchingResultId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask
                        .pending(
                                taskId,
                                patientId
                        )
                        .completed(
                                matchingResultId
                        )
        );

        MatchingTaskStatusResult result =
                service.getStatus(
                        taskId,
                        patientId
                );

        assertEquals(
                taskId,
                result.taskId()
        );

        assertEquals(
                MatchingTaskStatus.COMPLETED,
                result.taskStatus()
        );

        assertEquals(
                matchingResultId,
                result.matchingResultId()
        );
    }

    @Test
    @DisplayName("FAILED Task 조회 시 matchingResultId는 null이다")
    void failedTaskDoesNotExposeMatchingResultId() {

        UUID taskId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask
                        .pending(
                                taskId,
                                patientId
                        )
                        .failed()
        );

        MatchingTaskStatusResult result =
                service.getStatus(
                        taskId,
                        patientId
                );

        assertEquals(
                MatchingTaskStatus.FAILED,
                result.taskStatus()
        );

        assertNull(
                result.matchingResultId()
        );
    }

    @Test
    @DisplayName("존재하지 않는 taskId를 조회하면 TASK_NOT_FOUND 예외가 발생한다")
    void taskNotFound() {

        UUID taskId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.getStatus(
                                taskId,
                                patientId
                        )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_TASK_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("다른 환자의 Task를 조회하면 FORBIDDEN 예외가 발생한다")
    void otherPatientCannotReadTask() {

        UUID taskId = UUID.randomUUID();
        UUID ownerPatientId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        matchingTaskStore.save(
                MatchingTask.pending(
                        taskId,
                        ownerPatientId
                )
        );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.getStatus(
                                taskId,
                                requesterId
                        )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_TASK_FORBIDDEN,
                exception.getErrorCode()
        );
    }
}