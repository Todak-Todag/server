package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicePreferenceQueryRepository {
    Optional<CarePlanServicePreference> findById(UUID id);

    List<UUID> findIdsByPatientId(UUID patientId);

    List<CarePlanServicePreference> findAllByPlanServiceIds(
            List<UUID> planServiceIds
    );

    Page<ServicePreferenceView> search(
            UUID carePlanId,
            LocalDate preferredDate,
            Pageable pageable
    );
}
