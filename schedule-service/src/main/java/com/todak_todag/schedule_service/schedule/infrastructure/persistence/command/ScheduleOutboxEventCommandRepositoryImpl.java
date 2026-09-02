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

    @Override
    public ScheduleOutboxEvent save(ScheduleOutboxEvent scheduleOutboxEvent) {
        return springDataScheduleOutboxEventRepository.save(scheduleOutboxEvent);
    }

    @Override
    public Optional<ScheduleOutboxEvent> findById(UUID outboxEventId) {
        return springDataScheduleOutboxEventRepository.findById(outboxEventId);
    }
}
