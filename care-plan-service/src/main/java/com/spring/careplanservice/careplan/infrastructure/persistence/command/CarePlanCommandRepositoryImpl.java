package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaCarePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class CarePlanCommandRepositoryImpl implements CarePlanCommandRepository {
    private final JpaCarePlanRepository jpaCarePlanRepository;

    @Override
    public CarePlan save(CarePlan carePlan) {
        return jpaCarePlanRepository.save(carePlan);
    }

    @Override
    public boolean existsByDischargeId(UUID dischargeId) {
        return jpaCarePlanRepository.existsByDischargeIdAndDeletedAtIsNull(dischargeId);
    }
}
