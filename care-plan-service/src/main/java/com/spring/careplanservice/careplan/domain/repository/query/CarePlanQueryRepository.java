package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;

import java.util.Optional;
import java.util.UUID;

public interface CarePlanQueryRepository {
    Optional<CarePlan> findById(UUID id);
}
