package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ServiceMatchingAttemptCommandRepositoryImpl implements ServiceMatchingAttemptCommandRepository {

    private final SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Override
    public ServiceMatchingAttempt save(ServiceMatchingAttempt serviceMatchingAttempt) {
        return springDataServiceMatchingAttemptRepository.save(serviceMatchingAttempt);
    }
}
