package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;

import java.util.Optional;
import java.util.UUID;

public interface CarePlanOutboxEventCommandRepository {
    CarePlanOutboxEvent save(
            CarePlanOutboxEvent carePlanOutboxEvent
    );

    Optional<CarePlanOutboxEvent> findById(UUID outboxEventId);
}
