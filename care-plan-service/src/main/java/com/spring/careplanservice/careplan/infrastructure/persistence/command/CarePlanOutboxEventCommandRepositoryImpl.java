package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.SpringDataCarePlanOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CarePlanOutboxEventCommandRepositoryImpl implements CarePlanOutboxEventCommandRepository {
    private final SpringDataCarePlanOutboxEventRepository repository;

    @Override
    public CarePlanOutboxEvent save(
            CarePlanOutboxEvent carePlanOutboxEvent
    ) {
        return repository.save(carePlanOutboxEvent);
    }
}
