package com.todak_todag.discharge_service.discharge.domain.repository;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;

import java.util.Optional;
import java.util.UUID;

public interface DischargeRepository {

    Discharge save(Discharge discharge);

    Optional<Discharge> findById(UUID dischargeId);
}