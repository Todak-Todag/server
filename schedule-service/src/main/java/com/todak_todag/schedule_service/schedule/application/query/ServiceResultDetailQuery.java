package com.todak_todag.schedule_service.schedule.application.query;

import com.todak_todag.schedule_service.global.common.UserRole;

import java.util.UUID;

// 서비스 수행 결과 상세 조회 요청 파라미터
public record ServiceResultDetailQuery(
        UUID serviceResultId,
        UUID userId,
        UserRole role
) {
}
