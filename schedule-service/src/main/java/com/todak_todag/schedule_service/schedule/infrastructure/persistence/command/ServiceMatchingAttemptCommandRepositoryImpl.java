package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public Optional<ServiceMatchingAttempt> findLatestMatched(UUID servicePreferenceId) {
        return springDataServiceMatchingAttemptRepository
                .findFirstByServicePreferenceIdAndStatusAndDeletedAtIsNullOrderByMatchedAtDescCreatedAtDesc(
                        servicePreferenceId,
                        MatchingAttemptStatus.MATCHED
                );
    }
}
