package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.util.Optional;
import java.util.UUID;

public interface CarePlanCommandRepository {
    CarePlan save(CarePlan carePlan);

    boolean existsByDischargeId(UUID dischargeId);

    Optional<CarePlan> findById(UUID carePlanId);

    Optional<CarePlan> findByStatus(CarePlanStatus status);
}
