package com.todak_todag.discharge_service.discharge.domain.repository.query;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DischargeQueryRepository {

    Optional<Discharge> findById(UUID dischargeId);

    Page<Discharge> search(
            UUID hospitalStaffId,
            DischargeStatus status,
            LocalDate scheduledDate,
            Pageable pageable
    );
}