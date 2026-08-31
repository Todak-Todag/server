package com.spring.careplanservice.careplan.domain.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;

public interface CarePlanRepository {
    CarePlan save(CarePlan carePlan);
}
