package com.todak_todag.social_worker_service.matching.domain.repository.query;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerLoadProjection;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SocialWorkerMatchingQueryRepository {

    List<SocialWorkerLoadProjection> countBySocialWorkerIdsAndStatus(
            Set<UUID> socialWorkerIds,
            MatchingStatus status
    );

    Optional<SocialWorkerMatchingResult> findLatestByPatientId(
            UUID patientId
    );
}