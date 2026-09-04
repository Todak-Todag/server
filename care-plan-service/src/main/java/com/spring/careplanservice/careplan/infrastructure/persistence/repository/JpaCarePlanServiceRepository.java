package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCarePlanServiceRepository extends JpaRepository<CarePlanService, UUID> {
    Optional<CarePlanService> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByCarePlanIdAndProvideServiceIdAndCreatedByAndDeletedAtIsNull(
            UUID carePlanId,
            UUID provideServiceId,
            UUID createdBy
    );

    List<CarePlanService> findAllByCarePlanIdAndDeletedAtIsNull(
            UUID carePlanId
    );
}
