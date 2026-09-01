package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CarePlanQueryRepository {
    Optional<CarePlan> findById(UUID id);

    Optional<CarePlan> findByPatientIdAndStatuses(
            UUID patientId,
            Set<CarePlanStatus> statuses
    );
}
