package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.schedule.domain.entity.OutboxEventStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ScheduleOutboxEventQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ScheduleOutboxEventQueryRepositoryImpl implements ScheduleOutboxEventQueryRepository {

    private final SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    @Override
    public List<ScheduleOutboxEvent> findByStatus(OutboxEventStatus status, int limit) {
        return springDataScheduleOutboxEventRepository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(0, limit));
    }
}
