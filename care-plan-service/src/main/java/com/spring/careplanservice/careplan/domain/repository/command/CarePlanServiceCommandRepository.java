package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;

import java.util.List;

public interface CarePlanServiceCommandRepository {
    List<CarePlanService> saveAll(List<CarePlanService> carePlanServices);
}
