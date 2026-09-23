package com.spring.careplanservice.careplan.application.result;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeFindResult(
        UUID dischargeId,
        UUID patientId,
        LocalDate actualDate
) {
}
