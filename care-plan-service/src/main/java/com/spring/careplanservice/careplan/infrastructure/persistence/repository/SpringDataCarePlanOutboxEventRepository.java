package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCarePlanOutboxEventRepository extends JpaRepository<CarePlanOutboxEvent, UUID> {
}
