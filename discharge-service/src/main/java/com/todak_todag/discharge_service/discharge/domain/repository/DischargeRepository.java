package com.todak_todag.discharge_service.discharge.domain.repository;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;

public interface DischargeRepository {

    Discharge save(Discharge discharge);
}