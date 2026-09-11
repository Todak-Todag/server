package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;

import java.util.List;

public interface CarePlanOutboxEventQueryRepository {
    // 오래 대기한 이벤트부터 최대 limit건 조회
    List<CarePlanOutboxEvent> findByStatus(
            CarePlanOutboxEventStatus status,
            int limit
    );
}
