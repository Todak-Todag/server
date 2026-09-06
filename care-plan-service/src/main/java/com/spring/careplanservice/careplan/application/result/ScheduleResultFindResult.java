package com.spring.careplanservice.careplan.application.result;

import java.util.UUID;

public record ScheduleResultFindResult(
        UUID serviceResultId,
        UUID serviceScheduleId
) {
}
