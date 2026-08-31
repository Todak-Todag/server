package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;

public interface CarePlanCommandRepository {
    CarePlan save(CarePlan carePlan);
}
