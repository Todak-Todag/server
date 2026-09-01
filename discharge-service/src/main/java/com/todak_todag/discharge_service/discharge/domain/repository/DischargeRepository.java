package com.todak_todag.discharge_service.discharge.domain.repository;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DischargeRepository extends JpaRepository<Discharge, UUID> {
}