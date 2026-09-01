package com.todak_todag.discharge_service.discharge.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_discharges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Discharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID hospitalStaffId;

    @Column(nullable = false)
    private String hospitalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DischargeStatus status;

    @Column(nullable = false)
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
}