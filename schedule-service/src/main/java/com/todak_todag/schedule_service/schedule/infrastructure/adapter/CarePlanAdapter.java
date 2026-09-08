package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.infrastructure.client.care_plan.CarePlanClient;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanRangeInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanSummaryInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Override
    public List<UUID> findServicePreferenceIds(UUID patientId) {
        return carePlanClient.findServicePreferenceIds(patientId).data().content();
    }

    @Override
    public CarePlanSummary findCarePlanByPatient(UUID patientId) {
        CarePlanSummaryInternalResponse response =
                carePlanClient.findCarePlanByPatient(patientId).data();

        return new CarePlanSummary(response.carePlanId(), parseStatus(response.status()));
    }

    // 상대 서비스가 care_plan_status ENUM을 확장했을 때 역직렬화/변환에서 터지지 않도록 방어
    // 알 수 없는 상태는 null로 두어 CONFIRMED가 아닌 것으로 취급
    private CarePlanStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return CarePlanStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
