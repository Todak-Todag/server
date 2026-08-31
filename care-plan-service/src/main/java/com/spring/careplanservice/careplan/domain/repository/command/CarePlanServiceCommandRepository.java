package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;

public interface CarePlanServiceCommandRepository {
    CarePlanService save(CarePlanService carePlanService);
}
