package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCarePlanServiceRepository extends JpaRepository<CarePlanService, UUID> {
}
