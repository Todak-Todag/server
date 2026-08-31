package com.spring.careplanservice.domain.entity;


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
}
