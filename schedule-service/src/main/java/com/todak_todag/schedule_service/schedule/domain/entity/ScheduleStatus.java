package com.todak_todag.schedule_service.schedule.domain.entity;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;

public enum ScheduleStatus {

    SCHEDULED,
    RESCHEDULING,
    CHANGED,
    COMPLETED,
    CANCELED,
    NO_SHOW;

    // 목록 조회 status 필터 파싱 — 허용되지 않는 값이면 400
    public static ScheduleStatus fromFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ScheduleStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FILTER);
        }
    }
}
