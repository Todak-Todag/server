package com.spring.careplanservice.domain.repository;

import com.spring.careplanservice.domain.entity.CarePlanService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarePlanServiceRepository extends JpaRepository<CarePlanService, UUID> {
}
