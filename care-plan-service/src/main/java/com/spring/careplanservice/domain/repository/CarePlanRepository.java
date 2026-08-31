package com.spring.careplanservice.domain.repository;

import com.spring.careplanservice.domain.entity.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarePlanRepository extends JpaRepository<CarePlan, UUID> {
}
