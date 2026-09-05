package com.todak_todag.discharge_service.discharge.infrastructure.persistence.command;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.command.DischargeCommandRepository;
import com.todak_todag.discharge_service.discharge.infrastructure.persistence.JpaDischargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DischargeCommandRepositoryImpl implements DischargeCommandRepository {

    private final JpaDischargeRepository jpaDischargeRepository;

    @Override
    public Discharge save(Discharge discharge) {
        return jpaDischargeRepository.save(discharge);
    }
}