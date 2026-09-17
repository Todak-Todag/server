package com.todak_todag.schedule_service.schedule.application.result;

import java.util.UUID;

// [내부 API] 서비스 수행 결과 조회 결과
public record InternalServiceResultDetailResult(
        UUID carePlanId,
        UUID serviceResultId
) {

    public static InternalServiceResultDetailResult of(UUID carePlanId, UUID serviceResultId) {
        return new InternalServiceResultDetailResult(carePlanId, serviceResultId);
    }
}
