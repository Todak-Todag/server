package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataCarePlanOutboxEventRepository extends JpaRepository<CarePlanOutboxEvent, UUID> {
    List<CarePlanOutboxEvent> findByStatusOrderByCreatedAtAsc(
            CarePlanOutboxEventStatus status,
            Pageable pageable
    );
}
