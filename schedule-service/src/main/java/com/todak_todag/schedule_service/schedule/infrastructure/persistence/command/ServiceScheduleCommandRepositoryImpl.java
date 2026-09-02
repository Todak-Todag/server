package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ServiceScheduleCommandRepositoryImpl implements ServiceScheduleCommandRepository {

    private final SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Override
    public ServiceSchedule save(ServiceSchedule serviceSchedule) {
        return springDataServiceScheduleRepository.save(serviceSchedule);
    }
}
