package com.todak_todag.discharge_service.discharge.domain.entity;

import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;

import java.util.Map;

public enum DischargeStatus {

    SCHEDULED,
    POSTPONED,
    COMPLETED,
    CANCELED;

    public static DischargeStatus fromFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return DischargeStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_INPUT_VALUE,
                    Map.of(
                            "reason",
                            "유효하지 않은 퇴원 상태입니다."
                    )
            );
        }
    }
}