package com.todak_todag.schedule_service.schedule.application.query;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// 매칭 시도 내역 목록 조회 요청 파라미터
public record MatchingAttemptSearchQuery(
        UUID userId,
        MatchingAttemptStatus status,
        Pageable pageable
) {

    // status 기본값은 FAILED — Controller가 아닌 여기서 보정해 단위 테스트로 검증
    public static MatchingAttemptSearchQuery of(
            UUID userId,
            MatchingAttemptStatus status,
            Pageable pageable
    ) {
        return new MatchingAttemptSearchQuery(
                userId,
                status == null ? MatchingAttemptStatus.FAILED : status,
                pageable
        );
    }

    // FAILED 조회일 때만 "일정이 아직 생성되지 않은" 조건을 적용
    public boolean excludeAlreadyScheduled() {
        return status == MatchingAttemptStatus.FAILED;
    }
}
