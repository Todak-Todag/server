package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.global.common.UserRole;
import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.command.MatchingStatusChangeCommand;
import com.todak_todag.social_worker_service.matching.application.result.MatchingStatusChangeResult;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerMatchingRepository;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingStatusCommandService {

    private static final String END_STATUS = "ENDED";

    private final SocialWorkerMatchingRepository matchingRepository;

    @Transactional
    public MatchingStatusChangeResult changeStatus(
            MatchingStatusChangeCommand command
    ) {

        validateRequestedStatus(
                command.status()
        );

        SocialWorkerMatchingResult matchingResult =
                matchingRepository
                        .findById(command.matchingResultId())
                        .filter(result -> !result.isDeleted())
                        .orElseThrow(
                                () -> new BusinessException(
                                        MatchingErrorCode.MATCHING_NOT_FOUND,
                                        "사회복지사 매칭 상태 변경 실패"
                                )
                        );

        validateCurrentStatus(
                matchingResult
        );

        validatePermission(
                matchingResult,
                command
        );

        matchingResult.end();

        return new MatchingStatusChangeResult(
                matchingResult.getMatchingResultId(),
                matchingResult.getStatus()
        );
    }

    private void validateRequestedStatus(
            String requestedStatus
    ) {

        if (!END_STATUS.equals(requestedStatus)) {

            throw new BusinessException(
                    MatchingErrorCode.INVALID_MATCHING_STATUS,
                    "사회복지사 매칭 상태 변경 실패"
            );
        }
    }

    private void validateCurrentStatus(
            SocialWorkerMatchingResult matchingResult
    ) {

        if (matchingResult.getStatus()
                != MatchingStatus.ACTIVE) {

            throw new BusinessException(
                    MatchingErrorCode.INVALID_MATCHING_STATUS,
                    "사회복지사 매칭 상태 변경 실패"
            );
        }
    }

    private void validatePermission(
            SocialWorkerMatchingResult matchingResult,
            MatchingStatusChangeCommand command
    ) {

        UserRole role =
                command.requesterRole();

        if (role == UserRole.ADMIN) {
            return;
        }

        if (role == UserRole.SOCIAL_WORKER
                && command.requesterId().equals(
                        matchingResult.getSocialWorkerId()
                )) {
            return;
        }

        throw new BusinessException(
                MatchingErrorCode.MATCHING_STATUS_CHANGE_FORBIDDEN,
                "사회복지사 매칭 상태 변경 실패"
        );
    }
}