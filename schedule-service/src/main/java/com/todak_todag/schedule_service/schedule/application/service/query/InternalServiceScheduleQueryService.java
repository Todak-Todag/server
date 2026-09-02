package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.query.InternalServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.InternalServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalServiceScheduleQueryService {

    // Care Plan 고정값
    private static final int BATCH_QUERY_DAYS = 30;
    // 조회 대상 상태: SCHEDULED, RESCHEDULING
    private static final List<ScheduleStatus> QUERYABLE_STATUSES = List.of(
            ScheduleStatus.SCHEDULED,
            ScheduleStatus.RESCHEDULING
    );

    private final ServiceScheduleQueryRepository serviceScheduleRepository;

    // [내부 API] 서비스 제공자 일정 조회
    @Transactional(readOnly = true)
    public List<InternalServiceScheduleSearchResult> search(InternalServiceScheduleSearchQuery serviceScheduleSearchQuery) {
        // Care Plan 시작 날짜부터 종료 날짜까지의 범위 설정
        LocalDate endDate = serviceScheduleSearchQuery.startDate().plusDays(BATCH_QUERY_DAYS - 1);

        // startDate부터 BATCH_QUERY_DAYS(30일, 고정값)의 일정을 한 번에 반환
        // 이때, 일정 중 조회 대상 상태만 반환
        return serviceScheduleRepository.findSchedules(
                        serviceScheduleSearchQuery.serviceOfferingIds(),
                        serviceScheduleSearchQuery.startDate(),
                        endDate,
                        QUERYABLE_STATUSES
                ).stream()
                .map(InternalServiceScheduleSearchResult::from)
                .toList();
    }
}
