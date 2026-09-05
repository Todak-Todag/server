package com.todak_todag.discharge_service.discharge.domain.repository.command;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;

public interface DischargeCommandRepository {

    Discharge save(Discharge discharge);
}