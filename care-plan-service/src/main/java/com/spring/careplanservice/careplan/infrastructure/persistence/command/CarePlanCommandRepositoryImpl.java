package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class CarePlanCommandRepositoryImpl implements CarePlanCommandRepository {
    @Override
    public CarePlan save(
            CarePlan carePlan
    ) {
        return null;
    }
}
