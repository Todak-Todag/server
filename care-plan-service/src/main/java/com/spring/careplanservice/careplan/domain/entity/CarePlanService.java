package com.spring.careplanservice.careplan.domain.entity;


import com.spring.careplanservice.global.common.BaseCreateDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_care_plan_services")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarePlanService extends BaseCreateDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plan_service_id")
    private UUID id;

    @Column(name = "care_plan_id", nullable = false)
    private UUID carePlanId;

    @Column(name = "provide_service_id", nullable = false)
    private UUID provideServiceId;

    public static CarePlanService create(
            UUID carePlanId,
            UUID provideServiceId
    ) {
        CarePlanService carePlanService = new CarePlanService();

        carePlanService.carePlanId = carePlanId;
        carePlanService.provideServiceId = provideServiceId;

        return carePlanService;
    }
}
