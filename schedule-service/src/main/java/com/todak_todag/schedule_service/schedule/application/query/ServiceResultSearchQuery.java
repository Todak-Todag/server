package com.todak_todag.schedule_service.schedule.application.query;

import com.todak_todag.schedule_service.global.common.UserRole;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// 서비스 수행 결과 목록 조회 요청 파라미터
public record ServiceResultSearchQuery(
        UUID userId,
        UserRole role,
        Pageable pageable
) {
}
