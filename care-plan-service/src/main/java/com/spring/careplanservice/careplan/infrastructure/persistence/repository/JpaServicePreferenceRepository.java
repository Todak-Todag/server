package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaServicePreferenceRepository extends JpaRepository<CarePlanServicePreference, UUID> {
    Optional<CarePlanServicePreference> findByIdAndDeletedAtIsNull(UUID id);

    List<CarePlanServicePreference> findAllByPlanServiceIdAndDeletedAtIsNull(
            UUID planServiceId
    );

    List<CarePlanServicePreference> findAllByPlanServiceIdInAndDeletedAtIsNull(
            List<UUID> planServiceIds
    );
}
