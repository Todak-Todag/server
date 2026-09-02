package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.util.Optional;
import java.util.UUID;

public interface ServiceScheduleCommandRepository {

    ServiceSchedule save(ServiceSchedule serviceSchedule);

    // 단건 조회 — 소프트 삭제된 일정은 제외
    Optional<ServiceSchedule> findById(UUID serviceScheduleId);
}
