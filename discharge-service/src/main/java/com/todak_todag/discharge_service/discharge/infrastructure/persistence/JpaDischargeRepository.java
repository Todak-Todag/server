package com.todak_todag.discharge_service.discharge.infrastructure.persistence;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDischargeRepository extends JpaRepository<Discharge, UUID> {
}