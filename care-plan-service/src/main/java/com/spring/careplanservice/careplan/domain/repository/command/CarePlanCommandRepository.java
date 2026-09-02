package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;

import java.util.UUID;

public interface CarePlanCommandRepository {
    CarePlan save(CarePlan carePlan);

    boolean existsByDischargeId(UUID dischargeId);
}
