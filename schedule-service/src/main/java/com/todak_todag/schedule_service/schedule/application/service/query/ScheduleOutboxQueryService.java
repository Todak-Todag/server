package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.ScheduleOutboxEventResult;
import com.todak_todag.schedule_service.schedule.domain.entity.OutboxEventStatus;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ScheduleOutboxEventQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 아웃박스 조회 전용 서비스
@Service
@RequiredArgsConstructor
public class ScheduleOutboxQueryService {

    private final ScheduleOutboxEventQueryRepository scheduleOutboxEventQueryRepository;

    // 릴레이가 한 번의 폴링 주기에 처리할 PENDING 이벤트 목록을 조회해 Result로 변환
    // FAILED/SENT 상태는 이미 최종 처리된 것이므로 여기서 조회되지 않음
    @Transactional(readOnly = true)
    public List<ScheduleOutboxEventResult> findPending(int batchSize) {
        return scheduleOutboxEventQueryRepository.findByStatus(OutboxEventStatus.PENDING, batchSize).stream()
                .map(ScheduleOutboxEventResult::from)
                .toList();
    }
}
