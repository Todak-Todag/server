package com.todak_todag.social_worker_service.matching.application.service.query;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerLoadProjection;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerMatchingRepository;
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
public class MatchingSelectionService {

    private final SocialWorkerMatchingRepository matchingRepository;

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
                matchingRepository.countBySocialWorkerIdsAndStatus(
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
                                .thenComparing(UUID::toString)
                )
                .orElseThrow();
    }
}