package com.todak_todag.social_worker_service.matching.infrastructure.persistence.query;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerLoadProjection;
import com.todak_todag.social_worker_service.matching.domain.repository.query.SocialWorkerMatchingQueryRepository;
import com.todak_todag.social_worker_service.matching.infrastructure.persistence.SpringSocialWorkerMatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SocialWorkerMatchingQueryRepositoryImpl
        implements SocialWorkerMatchingQueryRepository {

    private final SpringSocialWorkerMatchingRepository
            springSocialWorkerMatchingRepository;

    @Override
    public List<SocialWorkerLoadProjection> countBySocialWorkerIdsAndStatus(
            Set<UUID> socialWorkerIds,
            MatchingStatus status
    ) {

        return springSocialWorkerMatchingRepository
                .countBySocialWorkerIdsAndStatus(
                        socialWorkerIds,
                        status
                );
    }

    @Override
    public Optional<SocialWorkerMatchingResult> findLatestByPatientId(
            UUID patientId
    ) {

        return springSocialWorkerMatchingRepository
                .findFirstByPatientIdAndDeletedAtIsNullOrderByRequestedAtDesc(
                        patientId
                );
    }
}