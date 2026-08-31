package com.spring.careplanservice.careplan.domain.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarePlanServiceRepository extends JpaRepository<CarePlanService, UUID> {
}
