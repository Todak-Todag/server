package com.todak_todag.schedule_service.schedule.domain.repository.query;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceScheduleQueryRepository {

    // [내부 API] 서비스 제공자 일정 조회
    List<ServiceSchedule> findSchedules(
            List<UUID> serviceOfferingIds,
            LocalDate startDate,
            LocalDate endDate,
            List<ScheduleStatus> statuses
    );

    // 단건 조회 — 소프트 삭제된 일정은 제외
    Optional<ServiceSchedule> findById(UUID serviceScheduleId);

    // 서비스 일정 목록 조회
    Page<ServiceSchedule> search(
            List<UUID> servicePreferenceIds,
            List<UUID> serviceOfferingIds,
            ScheduleStatus status,
            LocalDate date,
            Pageable pageable
    );
}
