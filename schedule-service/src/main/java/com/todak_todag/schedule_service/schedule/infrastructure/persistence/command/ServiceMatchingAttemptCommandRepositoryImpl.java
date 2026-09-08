package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceMatchingAttemptCommandRepositoryImpl implements ServiceMatchingAttemptCommandRepository {

    private final SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Override
    public ServiceMatchingAttempt save(ServiceMatchingAttempt serviceMatchingAttempt) {
        return springDataServiceMatchingAttemptRepository.save(serviceMatchingAttempt);
    }

    @Override
    public Optional<ServiceMatchingAttempt> findById(UUID matchingAttemptId) {
        return springDataServiceMatchingAttemptRepository.findByIdAndDeletedAtIsNull(matchingAttemptId);
    }

    @Override
    public boolean existsMatched(
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            Instant matchedAt
    ) {
        return springDataServiceMatchingAttemptRepository
                .existsByServicePreferenceIdAndServiceOfferingIdAndDateAndMatchedAtAndStatusAndDeletedAtIsNull(
                        servicePreferenceId,
                        serviceOfferingId,
                        date,
                        matchedAt,
                        MatchingAttemptStatus.MATCHED
                );
    }

    @Override
    public boolean existsFailed(
            UUID servicePreferenceId,
            LocalDate date,
            Instant failedAt
    ) {
        return springDataServiceMatchingAttemptRepository
                .existsByServicePreferenceIdAndDateAndFailedAtAndStatusAndDeletedAtIsNull(
                        servicePreferenceId,
                        date,
                        failedAt,
                        MatchingAttemptStatus.FAILED
                );
    }

    @Override
    public Optional<ServiceMatchingAttempt> findLatestMatched(UUID servicePreferenceId) {
        return springDataServiceMatchingAttemptRepository
                .findFirstByServicePreferenceIdAndStatusAndDeletedAtIsNullOrderByMatchedAtDescCreatedAtDesc(
                        servicePreferenceId,
                        MatchingAttemptStatus.MATCHED
                );
    }
}
