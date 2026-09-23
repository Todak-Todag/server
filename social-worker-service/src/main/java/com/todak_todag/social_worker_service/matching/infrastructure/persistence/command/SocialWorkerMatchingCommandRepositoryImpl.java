package com.todak_todag.social_worker_service.matching.infrastructure.persistence.command;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.command.SocialWorkerMatchingCommandRepository;
import com.todak_todag.social_worker_service.matching.infrastructure.persistence.SpringSocialWorkerMatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SocialWorkerMatchingCommandRepositoryImpl
        implements SocialWorkerMatchingCommandRepository {

    private final SpringSocialWorkerMatchingRepository
            springSocialWorkerMatchingRepository;

    @Override
    public SocialWorkerMatchingResult save(
            SocialWorkerMatchingResult matchingResult
    ) {
        return springSocialWorkerMatchingRepository.save(
                matchingResult
        );
    }

    @Override
    public Optional<SocialWorkerMatchingResult> findById(
            UUID matchingResultId
    ) {
        return springSocialWorkerMatchingRepository.findById(
                matchingResultId
        );
    }

    @Override
    public Optional<SocialWorkerMatchingResult> findByPatientIdAndStatus(
            UUID patientId,
            MatchingStatus status
    ) {
        return springSocialWorkerMatchingRepository
                .findByPatientIdAndStatusAndDeletedAtIsNull(
                        patientId,
                        status
                );
    }

    @Override
    public boolean existsByPatientIdAndStatusIn(
            UUID patientId,
            Collection<MatchingStatus> statuses
    ) {
        return springSocialWorkerMatchingRepository
                .existsByPatientIdAndStatusIn(
                        patientId,
                        statuses
                );
    }
}