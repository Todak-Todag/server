package com.todak_todag.schedule_service.schedule.application.port;

import java.util.List;
import java.util.UUID;

// schedule-service -> provider-service Internal API 호출 추상화
public interface ProviderOfferingPort {

    // serviceOfferingId 기준으로 해당 제공 서비스에 배정된 providerId를 조회
    UUID findAssignedProviderId(UUID serviceOfferingId);

    // providerId(요청자 userId)가 담당하는 모든 serviceOfferingId 목록을 조회
    List<UUID> findServiceOfferingIds(UUID providerId);
}
