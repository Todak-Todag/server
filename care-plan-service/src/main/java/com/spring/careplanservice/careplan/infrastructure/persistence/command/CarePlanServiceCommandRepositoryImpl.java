package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaCarePlanServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceCommandRepositoryImpl implements CarePlanServiceCommandRepository {
    private final JpaCarePlanServiceRepository jpaCarePlanServiceRepository;

    @Override
    public boolean existsByCarePlanIdAndProvideServiceId(
            UUID carePlanId,
            UUID provideServiceId
    ) {
        return jpaCarePlanServiceRepository
                .existsByCarePlanIdAndProvideServiceIdAndDeletedAtIsNull(
                        carePlanId,
                        provideServiceId
                );
    }

    @Override
    public CarePlanService save(CarePlanService carePlanService) {
        return jpaCarePlanServiceRepository.save(carePlanService);
    }

    @Override
    public List<CarePlanService> saveAll(
            List<CarePlanService> carePlanServices
    ) {
        return jpaCarePlanServiceRepository.saveAll(carePlanServices);
    }
}
