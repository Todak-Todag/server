package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.repository.CarePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class CarePlanRepositoryImpl implements CarePlanRepository {
    @Override
    public CarePlan save(
            CarePlan carePlan
    ) {
        return null;
    }
}
