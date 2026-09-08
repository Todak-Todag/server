package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerMatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingResultCommandService {

    private final SocialWorkerMatchingRepository matchingRepository;

    @Transactional
    public void activate(
            UUID matchingResultId,
            UUID socialWorkerId
    ) {
        SocialWorkerMatchingResult matchingResult =
                getMatchingResult(
                        matchingResultId
                );

        matchingResult.assign(
                socialWorkerId
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

        matchingResult.fail();
    }

    private SocialWorkerMatchingResult getMatchingResult(
            UUID matchingResultId
    ) {
        return matchingRepository
                .findById(matchingResultId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "사회복지사 매칭 결과를 찾을 수 없습니다. matchingResultId="
                                        + matchingResultId
                        )
                );
    }
}