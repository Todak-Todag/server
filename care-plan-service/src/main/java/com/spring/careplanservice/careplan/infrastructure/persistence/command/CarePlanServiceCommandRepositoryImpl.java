package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaCarePlanServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceCommandRepositoryImpl implements CarePlanServiceCommandRepository {
    private final JpaCarePlanServiceRepository jpaCarePlanServiceRepository;

    @Override
    public List<CarePlanService> saveAll(
            List<CarePlanService> carePlanServices
    ) {
        return jpaCarePlanServiceRepository.saveAll(carePlanServices);
    }
}
