package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;

public interface CarePlanOutboxEventCommandRepository {
    CarePlanOutboxEvent save(
            CarePlanOutboxEvent carePlanOutboxEvent
    );
}
