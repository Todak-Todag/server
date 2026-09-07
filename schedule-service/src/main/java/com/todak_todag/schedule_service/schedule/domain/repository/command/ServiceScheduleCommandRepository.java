package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.util.Collection;
import java.util.List;
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

    // 주어진 상태이면서 아직 수행 결과가 등록되지 않은 일정 수
    long countMissingResult(UUID carePlanId, Collection<ScheduleStatus> statuses);

    // 해당 서비스 희망 일정에서 재매칭을 기다리는(RESCHEDULING) 일정
    // ProviderMatched 수신이 신규 매칭인지 재매칭인지 가르는 기준이라 단건이 아닌 목록으로 반환
    // — 정상 상황에서는 0건(신규) 또는 1건(재매칭)이고, 2건 이상이면 데이터 이상이므로 호출 측에서 오류 반환
    List<ServiceSchedule> findRescheduling(UUID servicePreferenceId);
}
