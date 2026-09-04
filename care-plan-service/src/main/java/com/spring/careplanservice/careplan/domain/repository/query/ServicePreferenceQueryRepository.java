package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicePreferenceQueryRepository {
    Optional<CarePlanServicePreference> findById(UUID id);
    List<UUID> findIdsByPatientId(UUID patientId);
}
