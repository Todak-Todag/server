package com.todak_todag.discharge_service.discharge.application.command_service;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.DischargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DischargeCommandService {

    private final DischargeRepository dischargeRepository;

    @Transactional
    public DischargeCreateResult createDischarge(
            DischargeCreateCommand command
    ) {
        if (!command.scheduledDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "퇴원 예정일은 요청일 이후여야 합니다."
            );
        }

        Discharge discharge = Discharge.create(
                command.patientId(),
                command.hospitalStaffId(),
                command.hospitalName(),
                command.scheduledDate()
        );

        Discharge saved = dischargeRepository.save(discharge);

        return new DischargeCreateResult(saved.getId());
    }
}