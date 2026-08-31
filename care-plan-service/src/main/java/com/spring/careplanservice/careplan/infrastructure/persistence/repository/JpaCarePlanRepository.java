package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCarePlanRepository extends JpaRepository<CarePlan, UUID> {
}
