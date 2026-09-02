package com.todak_todag.discharge_service.discharge.infrastructure.adapter;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.DischargeRepository;
import com.todak_todag.discharge_service.discharge.infrastructure.persistence.JpaDischargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DischargePersistenceAdapter implements DischargeRepository {

    private final JpaDischargeRepository jpaDischargeRepository;

    @Override
    public Discharge save(Discharge discharge) {
        return jpaDischargeRepository.save(discharge);
    }
}