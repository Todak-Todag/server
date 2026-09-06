package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ServiceScheduleCommandRepository {

    ServiceSchedule save(ServiceSchedule serviceSchedule);

    // 단건 조회 — 소프트 삭제된 일정은 제외
    Optional<ServiceSchedule> findById(UUID serviceScheduleId);

    // 케어플랜의 "마지막 일정" 1건 조회 — CarePlanCompleted 발행 조건 판단용
    Optional<ServiceSchedule> findLastSchedule(UUID carePlanId);

    // 케어플랜에 아직 끝나지 않은 일정이 몇 건인지 — CarePlanCompleted 발행 조건 판단용
    // statuses에는 진행 중 상태(SCHEDULED / RESCHEDULING)를 넘김
    long countByCarePlanIdAndStatusIn(UUID carePlanId, Collection<ScheduleStatus> statuses);
}
