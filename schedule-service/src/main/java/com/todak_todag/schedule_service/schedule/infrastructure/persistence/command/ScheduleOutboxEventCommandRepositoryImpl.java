package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ScheduleOutboxEventCommandRepositoryImpl implements ScheduleOutboxEventCommandRepository {

    private final SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    // 즉시 플러시 — (event_type, aggregate_id) 유니크 위반을 커밋 시점이 아니라 호출 지점에서 잡기 위함
    // 적재는 커맨드 트랜잭션의 마지막 단계라 조기 플러시에 따르는 추가 비용이 사실상 없다
    @Override
    public ScheduleOutboxEvent save(ScheduleOutboxEvent scheduleOutboxEvent) {
        return springDataScheduleOutboxEventRepository.saveAndFlush(scheduleOutboxEvent);
    }

    @Override
    public Optional<ScheduleOutboxEvent> findById(UUID outboxEventId) {
        return springDataScheduleOutboxEventRepository.findById(outboxEventId);
    }

    @Override
    public boolean existsByEventTypeAndAggregateId(String eventType, UUID aggregateId) {
        return springDataScheduleOutboxEventRepository.existsByEventTypeAndAggregateId(eventType, aggregateId);
    }
}
