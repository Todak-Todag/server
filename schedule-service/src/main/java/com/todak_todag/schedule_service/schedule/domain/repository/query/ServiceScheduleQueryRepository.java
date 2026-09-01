package com.todak_todag.schedule_service.schedule.domain.repository.query;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ServiceScheduleQueryRepository {

    // [내부 API] 서비스 제공자 일정 조회
    List<ServiceSchedule> findAllByServiceOfferingIdInAndDateBetweenAndStatusInAndDeletedAtIsNull(
            List<UUID> serviceOfferingIds,
            LocalDate startDate,
            LocalDate endDate,
            List<ScheduleStatus> statuses
    );
}
