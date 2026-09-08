package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.service.async.MatchingAsyncProcessor;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerMatchingRepository;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.infrastructure.task.InMemoryMatchingTaskStore;
import com.todak_todag.social_worker_service.matching.infrastructure.task.MatchingTaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchingRequestCommandServiceTest {

    private SocialWorkerMatchingRepository matchingRepository;
    private MatchingAsyncProcessor matchingAsyncProcessor;
    private InMemoryMatchingTaskStore matchingTaskStore;

    private MatchingRequestCommandService service;

    @BeforeEach
    void setUp() {

        matchingRepository =
                mock(SocialWorkerMatchingRepository.class);

        matchingAsyncProcessor =
                mock(MatchingAsyncProcessor.class);

        matchingTaskStore =
                new InMemoryMatchingTaskStore();

        service =
                new MatchingRequestCommandService(
                        matchingRepository,
                        matchingTaskStore,
                        matchingAsyncProcessor
                );

        TransactionSynchronizationManager
                .initSynchronization();
    }

    @AfterEach
    void tearDown() {

        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .clearSynchronization();
        }
    }

    @Test
    @DisplayName("REQUESTED 또는 ACTIVE 매칭이 존재하면 재요청을 차단한다")
    void blocksRequestedOrActiveMatching() {

        UUID patientId = UUID.randomUUID();

        when(
                matchingRepository
                        .existsByPatientIdAndStatusIn(
                                eq(patientId),
                                anyCollection()
                        )
        ).thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.request(patientId)
                );

        assertEquals(
                MatchingErrorCode.MATCHING_ALREADY_IN_PROGRESS,
                exception.getErrorCode()
        );

        verify(
                matchingRepository,
                never()
        ).save(any());
    }

    @Test
    @DisplayName("중복 판단 상태에는 REQUESTED와 ACTIVE만 포함하고 FAILED는 포함하지 않는다")
    void failedStatusDoesNotBlockRematching() {

        UUID patientId = UUID.randomUUID();

        when(
                matchingRepository
                        .existsByPatientIdAndStatusIn(
                                eq(patientId),
                                anyCollection()
                        )
        ).thenReturn(false);

        UUID taskId =
                service.request(patientId);

        assertNotNull(taskId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MatchingStatus>> statusCaptor =
                ArgumentCaptor.forClass(Collection.class);

        verify(
                matchingRepository
        ).existsByPatientIdAndStatusIn(
                eq(patientId),
                statusCaptor.capture()
        );

        Collection<MatchingStatus> statuses =
                statusCaptor.getValue();

        assertTrue(
                statuses.contains(
                        MatchingStatus.REQUESTED
                )
        );

        assertTrue(
                statuses.contains(
                        MatchingStatus.ACTIVE
                )
        );

        assertFalse(
                statuses.contains(
                        MatchingStatus.FAILED
                )
        );

        verify(
                matchingRepository,
                times(1)
        ).save(any());
    }

    @Test
    @DisplayName("트랜잭션 커밋 이후 Task를 생성하고 비동기 매칭을 시작한다")
    void startsAsyncMatchingAfterCommit() {

        UUID patientId = UUID.randomUUID();

        when(
                matchingRepository
                        .existsByPatientIdAndStatusIn(
                                eq(patientId),
                                anyCollection()
                        )
        ).thenReturn(false);

        UUID taskId =
                service.request(patientId);

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager
                        .getSynchronizations();

        assertEquals(
                1,
                synchronizations.size()
        );

        verifyNoInteractions(
                matchingAsyncProcessor
        );

        synchronizations
                .get(0)
                .afterCommit();

        var task =
                matchingTaskStore
                        .findByTaskId(taskId)
                        .orElseThrow();

        assertEquals(
                MatchingTaskStatus.PENDING,
                task.status()
        );

        verify(
                matchingAsyncProcessor,
                times(1)
        ).process(
                eq(taskId),
                any(UUID.class),
                eq(patientId)
        );
    }
}