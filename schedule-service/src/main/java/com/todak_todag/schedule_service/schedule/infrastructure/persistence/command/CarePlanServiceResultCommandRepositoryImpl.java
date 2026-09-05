package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanServiceResultCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceResultCommandRepositoryImpl implements CarePlanServiceResultCommandRepository {

    private final SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Override
    public CarePlanServiceResult save(CarePlanServiceResult carePlanServiceResult) {
        return springDataCarePlanServiceResultRepository.save(carePlanServiceResult);
    }

    @Override
    public boolean existsByServiceScheduleId(UUID serviceScheduleId) {
        return springDataCarePlanServiceResultRepository.existsByServiceScheduleIdAndDeletedAtIsNull(serviceScheduleId);
    }
}
