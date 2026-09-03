package com.spring.careplanservice.careplan.application.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.time.LocalDate;
import java.util.UUID;

public record CarePlanSearchQuery(
        UUID patientId,
        CarePlanStatus status,
        LocalDate startDate,
        LocalDate finishDate,
        Integer page,
        Integer size
) {
}