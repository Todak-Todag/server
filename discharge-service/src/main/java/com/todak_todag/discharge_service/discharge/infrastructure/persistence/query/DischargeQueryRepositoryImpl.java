package com.todak_todag.discharge_service.discharge.infrastructure.persistence.query;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.query.DischargeQueryRepository;
import com.todak_todag.discharge_service.discharge.infrastructure.persistence.JpaDischargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DischargeQueryRepositoryImpl implements DischargeQueryRepository {

    private final JpaDischargeRepository jpaDischargeRepository;

    @Override
    public Optional<Discharge> findById(UUID dischargeId) {
        return jpaDischargeRepository.findById(dischargeId);
    }
}