package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

import java.util.Optional;
import java.util.UUID;

public interface CarePlanServicePreferenceQueryRepository {
    Optional<CarePlanServicePreference> findById(UUID id);
}
