package com.todak_todag.discharge_service.discharge.domain.entity;

import com.todak_todag.discharge_service.global.common.BaseAuditableEntity;
import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_discharges")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Discharge extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "discharge_id")
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID hospitalStaffId;

    @Column(nullable = false)
    private String hospitalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DischargeStatus status;

    private LocalDate scheduledDate;

    private LocalDate actualDate;

    private Discharge(
            UUID patientId,
            UUID hospitalStaffId,
            String hospitalName,
            LocalDate scheduledDate
    ) {
        this.patientId = patientId;
        this.hospitalStaffId = hospitalStaffId;
        this.hospitalName = hospitalName;
        this.scheduledDate = scheduledDate;
        this.status = DischargeStatus.SCHEDULED;
    }

    public static Discharge create(
            UUID patientId,
            UUID hospitalStaffId,
            String hospitalName,
            LocalDate scheduledDate
    ) {
        return new Discharge(
                patientId,
                hospitalStaffId,
                hospitalName,
                scheduledDate
        );
    }

    public void update(
            DischargeStatus status,
            LocalDate scheduledDate
    ) {
        validateModifiableStatus();

        if (status != null) {
            validateStatusTransition(status);
            this.status = status;
        }

        if (status == DischargeStatus.CANCELED || scheduledDate != null) {
            this.scheduledDate = scheduledDate;
        }
    }

    private void validateModifiableStatus() {
        if (this.status == DischargeStatus.COMPLETED
                || this.status == DischargeStatus.CANCELED) {
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
            DischargeStatus newStatus
    ) {
        boolean validTransition =
                (this.status == DischargeStatus.SCHEDULED
                        && (
                        newStatus == DischargeStatus.POSTPONED
                                || newStatus == DischargeStatus.CANCELED
                ))
                        ||
                        (this.status == DischargeStatus.POSTPONED
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

    public void complete(
            LocalDate actualDate
    ) {
        validateCompletableStatus();

        this.status = DischargeStatus.COMPLETED;
        this.actualDate = actualDate;
    }

    private void validateCompletableStatus() {
        if (this.status != DischargeStatus.SCHEDULED
                && this.status != DischargeStatus.POSTPONED) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION,
                    Map.of(
                            "reason",
                            "SCHEDULED 또는 POSTPONED 상태에서만 완료 처리가 가능합니다."
                    )
            );
        }
    }
}