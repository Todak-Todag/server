package com.todak_todag.discharge_service.discharge.domain.repository.query;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;

import java.util.Optional;
import java.util.UUID;

public interface DischargeQueryRepository {

    Optional<Discharge> findById(UUID dischargeId);
}