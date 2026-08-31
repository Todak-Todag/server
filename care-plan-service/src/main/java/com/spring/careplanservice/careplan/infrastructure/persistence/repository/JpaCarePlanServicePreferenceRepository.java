package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCarePlanServicePreferenceRepository extends JpaRepository<CarePlanServicePreference, UUID> {
}
