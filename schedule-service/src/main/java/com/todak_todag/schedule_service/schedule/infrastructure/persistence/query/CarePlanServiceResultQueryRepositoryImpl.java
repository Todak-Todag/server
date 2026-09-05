package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceResultQueryRepositoryImpl implements CarePlanServiceResultQueryRepository {

    private final SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Override
    public Optional<CarePlanServiceResult> findById(UUID serviceResultId) {
        return springDataCarePlanServiceResultRepository.findByServiceResultIdAndDeletedAtIsNull(serviceResultId);
    }
}
