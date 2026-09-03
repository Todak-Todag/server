package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.infrastructure.client.care_plan.CarePlanClient;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanRangeInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CarePlanAdapter implements CarePlanPort {

    private final CarePlanClient carePlanClient;

    @Override
    public CarePlanRange findCarePlanRange(UUID servicePreferenceId) {
        CarePlanRangeInternalResponse response =
                carePlanClient.findCarePlanRange(servicePreferenceId).data();

        return new CarePlanRange(response.carePlanId(), response.finishDate(), response.patientId());
    }
}
