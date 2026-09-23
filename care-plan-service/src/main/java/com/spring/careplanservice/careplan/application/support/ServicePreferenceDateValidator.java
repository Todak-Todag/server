package com.spring.careplanservice.careplan.application.support;

import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ServicePreferenceDateValidator {
    public void validate(
            LocalDate preferredDate,
            LocalDate startDate,
            LocalDate finishDate
    ) {
        LocalDate today = LocalDate.now();

        if (!preferredDate.isAfter(today)
                || preferredDate.isBefore(startDate)
                || preferredDate.isAfter(finishDate)) {

            throw new BusinessException(
                    ErrorCode.SERVICE_PREFERENCE_DATE_OUT_OF_RANGE
            );
        }
    }
}
