package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface JpaCarePlanRepository extends JpaRepository<CarePlan, UUID> {
    Optional<CarePlan> findByIdAndDeletedAtIsNull(UUID id);

    Optional<CarePlan> findByPatientIdAndStatusInAndDeletedAtIsNull(
            UUID patientId,
            Set<CarePlanStatus> statuses
    );
}
