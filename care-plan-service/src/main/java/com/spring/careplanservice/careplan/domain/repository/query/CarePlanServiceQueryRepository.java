package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;

import java.util.Optional;
import java.util.UUID;

public interface CarePlanServiceQueryRepository {
    Optional<CarePlanService> findById(UUID id);
}
