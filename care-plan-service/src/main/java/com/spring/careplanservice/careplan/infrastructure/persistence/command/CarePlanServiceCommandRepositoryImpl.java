package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceCommandRepositoryImpl implements CarePlanServiceCommandRepository {
    @Override
    public CarePlanService save(
            CarePlanService carePlanService
    ) {
        return null;
    }
}
