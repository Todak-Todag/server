package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.CarePlanServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceRepositoryImpl implements CarePlanServiceRepository {
    @Override
    public CarePlanService save(
            CarePlanService carePlanService
    ) {
        return null;
    }
}
