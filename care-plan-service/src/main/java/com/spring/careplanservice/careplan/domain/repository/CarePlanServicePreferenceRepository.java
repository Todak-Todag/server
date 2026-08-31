package com.spring.careplanservice.careplan.domain.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarePlanServicePreferenceRepository extends JpaRepository<CarePlanServicePreference, UUID> {
}
