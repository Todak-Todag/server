package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;

import java.time.Instant;
import java.util.List;

public interface CarePlanOutboxEventQueryRepository {
    // 오래 대기한 이벤트부터 최대 limit건 조회
    List<CarePlanOutboxEvent> findByStatus(
            CarePlanOutboxEventStatus status,
            int limit
    );

    // updatedAt 기준 threshold 이전부터 PROCESSING 상태로 멈춰 있는 이벤트 조회
    List<CarePlanOutboxEvent> findStuckProcessing(
            Instant updatedAtBefore,
            int limit
    );
}
