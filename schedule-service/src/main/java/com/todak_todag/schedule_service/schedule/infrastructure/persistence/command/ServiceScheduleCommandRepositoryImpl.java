package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceScheduleCommandRepositoryImpl implements ServiceScheduleCommandRepository {

    private final SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Override
    public ServiceSchedule save(ServiceSchedule serviceSchedule) {
        return springDataServiceScheduleRepository.save(serviceSchedule);
    }

    @Override
    public Optional<ServiceSchedule> findById(UUID serviceScheduleId) {
        return springDataServiceScheduleRepository.findByIdAndDeletedAtIsNull(serviceScheduleId);
    }

    @Override
    public Optional<ServiceSchedule> findLastSchedule(UUID carePlanId) {
        return springDataServiceScheduleRepository
                .findFirstByCarePlanIdAndStatusNotAndDeletedAtIsNullOrderByFinishedAtDescCreatedAtDesc(
                        carePlanId,
                        ScheduleStatus.CHANGED
                );
    }

    @Override
    public long countByCarePlanIdAndStatusIn(UUID carePlanId, Collection<ScheduleStatus> statuses) {
        return springDataServiceScheduleRepository.countByCarePlanIdAndStatusInAndDeletedAtIsNull(carePlanId, statuses);
    }

    @Override
    public long countMissingResult(UUID carePlanId, Collection<ScheduleStatus> statuses) {
        return springDataServiceScheduleRepository.countMissingResult(carePlanId, statuses);
    }

    @Override
    public List<ServiceSchedule> findRescheduling(UUID servicePreferenceId) {
        return springDataServiceScheduleRepository.findByServicePreferenceIdAndStatusAndDeletedAtIsNull(
                servicePreferenceId,
                ScheduleStatus.RESCHEDULING
        );
    }
}
