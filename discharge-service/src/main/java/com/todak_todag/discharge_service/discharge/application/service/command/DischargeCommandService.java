package com.todak_todag.discharge_service.discharge.application.service.command;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.command.DischargeUpdateCommand;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeUpdateResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.command.DischargeCommandRepository;
import com.todak_todag.discharge_service.discharge.domain.repository.query.DischargeQueryRepository;
import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DischargeCommandService {

    private final DischargeCommandRepository dischargeCommandRepository;
    private final DischargeQueryRepository dischargeQueryRepository;

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

        Discharge saved = dischargeCommandRepository.save(discharge);

        return new DischargeCreateResult(saved.getId());
    }

    @Transactional
    public DischargeUpdateResult updateDischarge(
            DischargeUpdateCommand command
    ) {
        Discharge discharge =
                dischargeQueryRepository.findById(command.dischargeId())
                        .orElseThrow(
                                () -> new BusinessException(
                                        ErrorCode.DISCHARGE_NOT_FOUND,
                                        Map.of(
                                                "reason",
                                                "퇴원건을 찾을 수 없습니다."
                                        )
                                )
                        );

        validateUpdatePermission(
                discharge,
                command.hospitalStaffId()
        );

        validateScheduledDate(
                command.scheduledDate()
        );

        discharge.updateScheduledDate(
                command.scheduledDate()
        );

        return DischargeUpdateResult.from(discharge);
    }

    private void validateUpdatePermission(
            Discharge discharge,
            UUID hospitalStaffId
    ) {
        if (!discharge.getHospitalStaffId().equals(hospitalStaffId)) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN,
                    Map.of(
                            "reason",
                            "퇴원건 수정 권한이 없습니다."
                    )
            );
        }
    }

    private void validateScheduledDate(
            LocalDate scheduledDate
    ) {
        if (!scheduledDate.isAfter(LocalDate.now())) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_INVALID_SCHEDULED_DATE,
                    Map.of(
                            "reason",
                            "퇴원 예정일은 요청일 이후여야 합니다."
                    )
            );
        }
    }
}