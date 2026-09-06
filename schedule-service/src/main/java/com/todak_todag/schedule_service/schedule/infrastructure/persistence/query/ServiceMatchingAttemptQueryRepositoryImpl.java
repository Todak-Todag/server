package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceMatchingAttemptQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceMatchingAttemptQueryRepositoryImpl implements ServiceMatchingAttemptQueryRepository {

    private final SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Override
    public Optional<ServiceMatchingAttempt> findById(UUID matchingAttemptId) {
        return springDataServiceMatchingAttemptRepository.findByIdAndDeletedAtIsNull(matchingAttemptId);
    }
}
