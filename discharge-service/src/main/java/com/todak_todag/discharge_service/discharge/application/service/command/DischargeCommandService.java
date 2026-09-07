package com.todak_todag.discharge_service.discharge.application.service.command;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.command.DischargeUpdateCommand;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeUpdateResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
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

        Discharge saved =
                dischargeCommandRepository.save(discharge);

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

        validateUpdateRequest(command);

        validateCurrentStatus(
                discharge.getStatus()
        );

        validateStatusTransition(
                discharge.getStatus(),
                command.status()
        );

        validateScheduledDate(
                command.status(),
                command.scheduledDate()
        );

        discharge.update(
                command.status(),
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

    private void validateUpdateRequest(
            DischargeUpdateCommand command
    ) {
        if (command.status() == null
                && command.scheduledDate() == null) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_INVALID_UPDATE_REQUEST,
                    Map.of(
                            "reason",
                            "status와 scheduledDate 중 하나 이상을 입력해야 합니다."
                    )
            );
        }
    }

    private void validateCurrentStatus(
            DischargeStatus currentStatus
    ) {
        if (currentStatus == DischargeStatus.COMPLETED
                || currentStatus == DischargeStatus.CANCELED) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION,
                    Map.of(
                            "reason",
                            "완료되거나 취소된 퇴원건은 수정할 수 없습니다."
                    )
            );
        }
    }

    private void validateStatusTransition(
            DischargeStatus currentStatus,
            DischargeStatus newStatus
    ) {
        if (newStatus == null) {
            return;
        }

        boolean validTransition =
                (currentStatus == DischargeStatus.SCHEDULED
                        && (
                        newStatus == DischargeStatus.POSTPONED
                                || newStatus == DischargeStatus.CANCELED
                ))
                        ||
                        (currentStatus == DischargeStatus.POSTPONED
                                && newStatus == DischargeStatus.CANCELED);

        if (!validTransition) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION,
                    Map.of(
                            "reason",
                            "허용되지 않은 퇴원 상태 변경입니다."
                    )
            );
        }
    }

    private void validateScheduledDate(
            DischargeStatus status,
            LocalDate scheduledDate
    ) {
        if (status == DischargeStatus.POSTPONED
                && scheduledDate == null) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_INVALID_SCHEDULED_DATE,
                    Map.of(
                            "reason",
                            "퇴원 연기 시 변경할 예정 퇴원일은 필수입니다."
                    )
            );
        }

        if (scheduledDate != null
                && !scheduledDate.isAfter(LocalDate.now())) {
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