package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;

import java.util.Optional;
import java.util.UUID;

// 아웃박스 레코드의 상태 변경(적재/갱신)만 담당
public interface ScheduleOutboxEventCommandRepository {

    // 신규 적재(enqueue)와 상태 갱신(markSent/recordFailure 이후 저장) 양쪽에서 공용으로 사용
    ScheduleOutboxEvent save(ScheduleOutboxEvent scheduleOutboxEvent);

    // markSent/recordFailure 처리 전, 최신 상태를 다시 읽어오기 위한 단건 조회
    Optional<ScheduleOutboxEvent> findById(UUID outboxEventId);
}
