package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicePreferenceCommandRepository {
    CarePlanServicePreference save(
            CarePlanServicePreference carePlanServicePreference
    );

    Optional<CarePlanServicePreference> findById(UUID id);

    List<CarePlanServicePreference> findAllByPlanServiceIds(
            List<UUID> planServiceIds
    );
}
