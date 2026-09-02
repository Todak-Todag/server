package com.spring.careplanservice.careplan.application.support;

import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CarePlanOwnerValidator {
    public void validate(
            UUID userId,
            UUID patientId
    ) {
        if (!userId.equals(patientId)) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }
    }
}
