package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;

import java.time.LocalDate;
import java.util.UUID;

public record CarePlanFindByServicePreferenceResult(
        UUID id,
        LocalDate finishDate
) {
    public static CarePlanFindByServicePreferenceResult from(
            CarePlan carePlan
    ) {
        return new CarePlanFindByServicePreferenceResult(
                carePlan.getId(),
                carePlan.getFinishDate()
        );
    }
}
