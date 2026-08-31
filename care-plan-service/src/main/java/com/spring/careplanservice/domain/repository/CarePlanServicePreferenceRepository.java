package com.spring.careplanservice.domain.repository;

import com.spring.careplanservice.domain.entity.CarePlanServicePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarePlanServicePreferenceRepository extends JpaRepository<CarePlanServicePreference, UUID> {
}
