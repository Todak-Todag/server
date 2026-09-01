package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;

import java.time.LocalDate;
import java.util.UUID;

public record CarePlanFindByPreferenceResult(
        UUID carePlanId,
        LocalDate finishDate
) {
    public static CarePlanFindByPreferenceResult from(
            CarePlan carePlan
    ) {
        return new CarePlanFindByPreferenceResult(
                carePlan.getId(),
                carePlan.getFinishDate()
        );
    }
}
