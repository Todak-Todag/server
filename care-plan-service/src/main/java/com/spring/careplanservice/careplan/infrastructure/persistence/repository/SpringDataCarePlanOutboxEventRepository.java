package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataCarePlanOutboxEventRepository extends JpaRepository<CarePlanOutboxEvent, UUID> {
    List<CarePlanOutboxEvent> findByStatusOrderByCreatedAtAsc(
            CarePlanOutboxEventStatus status,
            Pageable pageable
    );

    // 선점(PROCESSING) 이후 오래도록 갱신되지 않은, 즉 죽은 인스턴스에
    // 선점된 채 방치된 이벤트를 찾기 위한 조회
    List<CarePlanOutboxEvent> findByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            CarePlanOutboxEventStatus status,
            Instant updatedAtBefore,
            Pageable pageable
    );
}
