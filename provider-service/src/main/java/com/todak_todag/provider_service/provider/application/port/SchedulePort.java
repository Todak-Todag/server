package com.todak_todag.provider_service.provider.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SchedulePort {

    boolean existsConfirmedSchedule(UUID serviceOfferingId);

    // 후보 전체의 기존 일정을 startDate부터 30일치 한 번에 조회한다
    List<ScheduleSlot> findSchedules(List<UUID> serviceOfferingIds, LocalDate startDate);
}