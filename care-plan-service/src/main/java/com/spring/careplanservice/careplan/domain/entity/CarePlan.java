package com.spring.careplanservice.careplan.domain.entity;


import com.spring.careplanservice.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_care_plan")
public class CarePlan extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "care_plan_id")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "discharge_id", nullable = false)
    private UUID dischargeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CarePlanStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "finish_date", nullable = false)
    private LocalDate finishDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public static CarePlan create(
            UUID patientId,
            UUID dischargeId,
            LocalDate startDate,
            LocalDate finishDate,
            String note
    ) {
        CarePlan carePlan = new CarePlan();

        carePlan.patientId = patientId;
        carePlan.dischargeId = dischargeId;
        carePlan.status = CarePlanStatus.UNDER_REVIEW;
        carePlan.startDate = startDate;
        carePlan.finishDate = finishDate;
        carePlan.note = note;

        return carePlan;
    }
}
