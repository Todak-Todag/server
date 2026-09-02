package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;

import java.util.List;
import java.util.UUID;

public interface CarePlanServiceCommandRepository {
    CarePlanService save(CarePlanService carePlanService);

    List<CarePlanService> saveAll(List<CarePlanService> carePlanServices);

    boolean existsByCarePlanIdAndProvideServiceIdAndCreatedBy(
            UUID carePlanId,
            UUID provideServiceId,
            UUID createdBy
    );
}
