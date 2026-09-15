package com.spring.careplanservice.careplan.application.support;

import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.event.ScheduleStatus;
import com.spring.careplanservice.careplan.application.result.ScheduleResultFindResult;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CarePlanCompletedEventValidator {
    public void validatePayload(
            CarePlanCompletedEvent event
    ) {
        if (event.status() == ScheduleStatus.CANCELED) {
            return;
        }

        if (event.serviceResultId() == null) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_COMPLETED_EVENT_INVALID
            );
        }
    }

    public void validateCarePlanId(
            CarePlanCompletedEvent event,
            ScheduleResultFindResult scheduleResult
    ) {
        if (!event.carePlanId().equals(scheduleResult.carePlanId())) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_COMPLETED_EVENT_CARE_PLAN_MISMATCH
            );
        }
    }
}
