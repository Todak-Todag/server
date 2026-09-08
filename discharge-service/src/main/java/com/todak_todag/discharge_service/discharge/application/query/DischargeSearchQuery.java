package com.todak_todag.discharge_service.discharge.application.query;

import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeSearchQuery(
        UUID hospitalStaffId,
        DischargeStatus status,
        LocalDate scheduledDate,
        Pageable pageable
) {
}