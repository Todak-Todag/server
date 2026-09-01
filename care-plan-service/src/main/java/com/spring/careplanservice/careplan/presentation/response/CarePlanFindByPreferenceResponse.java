package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanFindByPreferenceResult;

import java.time.LocalDate;
import java.util.UUID;

public record CarePlanFindByPreferenceResponse(
        UUID carePlanId,
        LocalDate finishDate
) {

    public static CarePlanFindByPreferenceResponse from(
            CarePlanFindByPreferenceResult carePlanFindByPreferenceResult
    ) {
        return new CarePlanFindByPreferenceResponse(
                carePlanFindByPreferenceResult.carePlanId(),
                carePlanFindByPreferenceResult.finishDate()
        );
    }
}