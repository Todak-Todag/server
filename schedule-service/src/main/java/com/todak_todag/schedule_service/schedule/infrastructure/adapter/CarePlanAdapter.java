package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.infrastructure.client.InternalApiResponses;
import com.todak_todag.schedule_service.schedule.infrastructure.client.care_plan.CarePlanClient;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanRangeInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanSummaryInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.ServicePreferenceIdListInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CarePlanAdapter implements CarePlanPort {

    private final CarePlanClient carePlanClient;

    // 호출 실패(non-2xx/연결 실패)는 InternalApiErrorDecoder와 GlobalExceptionHandler가 변환
    // 여기서는 2xx인데 본문이 비어 오는 계약 위반만 걸러 NPE(=500)가 되지 않게 함
    @Override
    public CarePlanRange findCarePlanRange(UUID servicePreferenceId) {
        CarePlanRangeInternalResponse response = InternalApiResponses.requireData(
                carePlanClient.findCarePlanRange(servicePreferenceId), "care-plan-service.findCarePlanRange");

        return new CarePlanRange(response.carePlanId(), response.finishDate(), response.patientId());
    }

    @Override
    public List<UUID> findServicePreferenceIds(UUID patientId) {
        ServicePreferenceIdListInternalResponse response = InternalApiResponses.requireData(
                carePlanClient.findServicePreferenceIds(patientId), "care-plan-service.findServicePreferenceIds");

        return InternalApiResponses.require(response.content(), "care-plan-service.findServicePreferenceIds.content");
    }

    @Override
    public CarePlanSummary findCarePlanByPatient(UUID patientId) {
        CarePlanSummaryInternalResponse response = InternalApiResponses.requireData(
                carePlanClient.findCarePlanByPatient(patientId), "care-plan-service.findCarePlanByPatient");

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
