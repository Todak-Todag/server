package com.spring.careplanservice.careplan.domain.entity;


import com.spring.careplanservice.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_care_plan_service_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarePlanServicePreference extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_preference_id")
    private UUID id;

    @Column(name = "plan_service_id", nullable = false)
    private UUID planServiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time_slot", nullable = false)
    private PreferredTimeSlot preferredTimeSlot;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    public static CarePlanServicePreference create(
            UUID planServiceId,
            LocalDate preferredDate,
            PreferredTimeSlot preferredTimeSlot
    ) {
        CarePlanServicePreference preference =
                new CarePlanServicePreference();

        preference.planServiceId = planServiceId;
        preference.preferredDate = preferredDate;
        preference.preferredTimeSlot = preferredTimeSlot;

        return preference;
    }

    public void updatePreference(
            LocalDate preferredDate,
            PreferredTimeSlot preferredTimeSlot
    ) {
        this.preferredDate = preferredDate;
        this.preferredTimeSlot = preferredTimeSlot;
    }

    public void delete(UUID deletedBy) {
        markDeleted(deletedBy);
    }
}
