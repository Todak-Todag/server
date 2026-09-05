package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;

public interface ServiceMatchingAttemptCommandRepository {

    ServiceMatchingAttempt save(ServiceMatchingAttempt serviceMatchingAttempt);
}
