package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface JpaCarePlanRepository extends JpaRepository<CarePlan, UUID> {
    Optional<CarePlan> findFirstByPatientIdAndStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID patientId,
            Collection<CarePlanStatus> statuses
    );
}
