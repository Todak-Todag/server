package com.spring.careplanservice.careplan.domain.entity;

public enum CarePlanOutboxEventStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
