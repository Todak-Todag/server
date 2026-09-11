package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.command.SocialWorkerMatchingCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingResultCommandService {

    private final SocialWorkerMatchingCommandRepository matchingCommandRepository;

    @Transactional
    public void activate(
            UUID matchingResultId,
            UUID socialWorkerId
    ) {

        SocialWorkerMatchingResult matchingResult =
                getMatchingResult(
                        matchingResultId
                );

        if (matchingResult.getStatus()
                != MatchingStatus.REQUESTED) {

            throw new IllegalStateException(
                    "REQUESTED 상태의 매칭만 배정할 수 있습니다."
            );
        }

        matchingResult.setSocialWorkerId(
                socialWorkerId
        );

        matchingResult.setStatus(
                MatchingStatus.ACTIVE
        );

        matchingResult.setAssignedAt(
                Instant.now()
        );
    }

    @Transactional
    public void fail(
            UUID matchingResultId
    ) {

        SocialWorkerMatchingResult matchingResult =
                getMatchingResult(
                        matchingResultId
                );

        if (matchingResult.getStatus()
                != MatchingStatus.REQUESTED) {
            return;
        }

        matchingResult.setStatus(
                MatchingStatus.FAILED
        );
    }

    private SocialWorkerMatchingResult getMatchingResult(
            UUID matchingResultId
    ) {

        return matchingCommandRepository
                .findById(
                        matchingResultId
                )
                .orElseThrow(
                        () -> new IllegalStateException(
                                "사회복지사 매칭 결과를 찾을 수 없습니다. matchingResultId="
                                        + matchingResultId
                        )
                );
    }
}