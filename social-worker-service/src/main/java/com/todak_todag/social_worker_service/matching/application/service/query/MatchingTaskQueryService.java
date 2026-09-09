package com.todak_todag.social_worker_service.matching.application.service.query;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.result.MatchingTaskStatusResult;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStore;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingTaskQueryService {

    private final MatchingTaskStore matchingTaskStore;

    public MatchingTaskStatusResult getStatus(
            UUID taskId,
            UUID requesterId
    ) {

        MatchingTask task =
                matchingTaskStore
                        .findByTaskId(taskId)
                        .orElseThrow(
                                () -> new BusinessException(
                                        MatchingErrorCode.MATCHING_TASK_NOT_FOUND,
                                        "사회복지사 매칭 처리 현황 조회 실패"
                                )
                        );

        validateOwner(
                task,
                requesterId
        );

        return MatchingTaskStatusResult.from(
                task
        );
    }

    private void validateOwner(
            MatchingTask task,
            UUID requesterId
    ) {

        if (!task.patientId().equals(requesterId)) {
            throw new BusinessException(
                    MatchingErrorCode.MATCHING_TASK_FORBIDDEN,
                    "사회복지사 매칭 처리 현황 조회 실패"
            );
        }
    }
}