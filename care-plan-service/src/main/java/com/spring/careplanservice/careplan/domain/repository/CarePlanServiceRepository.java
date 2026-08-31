package com.spring.careplanservice.careplan.domain.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;

public interface CarePlanServiceRepository {
    CarePlanService save(CarePlanService carePlanService);
}
