package com.todak_todag.schedule_service.schedule.application.query;

import com.todak_todag.schedule_service.global.common.UserRole;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// 서비스 수행 결과 목록 조회 요청 파라미터 (08번 문서)
// 01번(ServiceScheduleSearchQuery)과 동일한 구조지만, 08번 문서 Request 표에는 status/date 필터가 없어 페이지네이션 정보만 전달한다.
public record ServiceResultSearchQuery(
        UUID userId,
        UserRole role,
        Pageable pageable
) {
}
