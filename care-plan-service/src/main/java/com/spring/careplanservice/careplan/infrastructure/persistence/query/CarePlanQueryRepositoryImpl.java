package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaCarePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class CarePlanQueryRepositoryImpl implements CarePlanQueryRepository {
    private final JpaCarePlanRepository jpaCarePlanRepository;

    @Override
    public Optional<CarePlan> findById(UUID id) {
        return jpaCarePlanRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<CarePlan> findByPatientIdAndStatuses(
            UUID patientId,
            Set<CarePlanStatus> statuses
    ) {
        return jpaCarePlanRepository
                .findByPatientIdAndStatusInAndDeletedAtIsNull(
                        patientId,
                        statuses
                );
    }

}
