package com.todak_todag.social_worker_service.matching.domain.repository.command;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SocialWorkerMatchingCommandRepository {

    SocialWorkerMatchingResult save(
            SocialWorkerMatchingResult matchingResult
    );

    Optional<SocialWorkerMatchingResult> findById(
            UUID matchingResultId
    );

    boolean existsByPatientIdAndStatusIn(
            UUID patientId,
            Collection<MatchingStatus> statuses
    );
}