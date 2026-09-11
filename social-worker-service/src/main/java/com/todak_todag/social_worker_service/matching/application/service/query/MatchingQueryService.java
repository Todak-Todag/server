package com.todak_todag.social_worker_service.matching.application.service.query;

import com.todak_todag.social_worker_service.global.common.UserRole;
import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.query.MatchingResultQuery;
import com.todak_todag.social_worker_service.matching.application.result.MatchingResultQueryResult;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerLoadProjection;
import com.todak_todag.social_worker_service.matching.domain.repository.query.SocialWorkerMatchingQueryRepository;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingQueryService {

    private final SocialWorkerMatchingQueryRepository matchingQueryRepository;

    public UUID select(
            Set<UUID> candidateIds
    ) {

        if (candidateIds == null
                || candidateIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "사회복지사 후보가 존재하지 않습니다."
            );
        }

        List<SocialWorkerLoadProjection> loads =
                matchingQueryRepository
                        .countBySocialWorkerIdsAndStatus(
                                candidateIds,
                                MatchingStatus.ACTIVE
                        );

        Map<UUID, Long> loadMap =
                new HashMap<>();

        for (SocialWorkerLoadProjection load : loads) {

            loadMap.put(
                    load.getSocialWorkerId(),
                    load.getActiveCount()
            );
        }

        return candidateIds.stream()
                .min(
                        Comparator
                                .comparingLong(
                                        (UUID id) ->
                                                loadMap.getOrDefault(
                                                        id,
                                                        0L
                                                )
                                )
                                .thenComparing(
                                        UUID::toString
                                )
                )
                .orElseThrow();
    }

    public MatchingResultQueryResult getResult(
            MatchingResultQuery query
    ) {

        SocialWorkerMatchingResult matchingResult =
                matchingQueryRepository
                        .findById(
                                query.matchingResultId()
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        MatchingErrorCode.MATCHING_RESULT_NOT_FOUND,
                                        "사회복지사 매칭 결과 조회 실패"
                                )
                        );

        validatePermission(
                matchingResult,
                query
        );

        return MatchingResultQueryResult.from(
                matchingResult
        );
    }

    private void validatePermission(
            SocialWorkerMatchingResult matchingResult,
            MatchingResultQuery query
    ) {

        UserRole requesterRole =
                query.requesterRole();

        if (requesterRole == UserRole.ADMIN) {
            return;
        }

        if (requesterRole == UserRole.PATIENT
                && query.requesterId().equals(
                matchingResult.getPatientId()
        )) {
            return;
        }

        if (requesterRole == UserRole.SOCIAL_WORKER
                && query.requesterId().equals(
                matchingResult.getSocialWorkerId()
        )) {
            return;
        }

        throw new BusinessException(
                MatchingErrorCode.MATCHING_QUERY_FORBIDDEN,
                "사회복지사 매칭 결과 조회 실패"
        );
    }
}