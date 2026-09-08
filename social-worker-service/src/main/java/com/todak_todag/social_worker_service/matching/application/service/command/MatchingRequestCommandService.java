package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.service.async.MatchingAsyncProcessor;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.command.SocialWorkerMatchingCommandRepository;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.infrastructure.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.infrastructure.task.MatchingTaskStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingRequestCommandService {

    private static final List<MatchingStatus> BLOCKING_STATUSES =
            List.of(
                    MatchingStatus.REQUESTED,
                    MatchingStatus.ACTIVE
            );

    private final SocialWorkerMatchingCommandRepository matchingCommandRepository;
    private final MatchingTaskStore matchingTaskStore;
    private final MatchingAsyncProcessor matchingAsyncProcessor;

    @Transactional
    public UUID request(
            UUID patientId
    ) {

        boolean alreadyExists =
                matchingCommandRepository
                        .existsByPatientIdAndStatusIn(
                                patientId,
                                BLOCKING_STATUSES
                        );

        if (alreadyExists) {
            throw new BusinessException(
                    MatchingErrorCode.MATCHING_ALREADY_IN_PROGRESS,
                    "사회복지사 매칭 요청 실패"
            );
        }

        SocialWorkerMatchingResult matchingResult =
                SocialWorkerMatchingResult
                        .requested(
                                patientId
                        );

        matchingCommandRepository.save(
                matchingResult
        );

        UUID taskId =
                UUID.randomUUID();

        UUID matchingResultId =
                matchingResult
                        .getMatchingResultId();

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                matchingTaskStore.save(
                                        MatchingTask.pending(
                                                taskId
                                        )
                                );

                                matchingAsyncProcessor.process(
                                        taskId,
                                        matchingResultId,
                                        patientId
                                );
                            }
                        }
                );

        return taskId;
    }
}