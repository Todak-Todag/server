package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceScheduleCommandService {

    // 일정 시작 24시간 전까지만 변경 가능
    private static final long RESCHEDULE_DEADLINE_HOURS = 24;

    private final ServiceScheduleCommandRepository serviceScheduleCommandRepository;
    private final CarePlanPort carePlanPort;
    private final ProviderReMatchEventPort providerReMatchEventPort;

    // 서비스 일정 변경
    // 동기 처리 범위: 검증 + status를 RESCHEDULING으로 변경 + ProviderReMatched 이벤트 발행
    // TODO: 재매칭 결과를 받아 CHANGED/SCHEDULED로 전환하는 후속처리 진행 필요
    @Transactional
    public ServiceScheduleRescheduleResult reschedule(ServiceScheduleRescheduleCommand rescheduleCommandcommand) {
        ServiceSchedule serviceSchedule = serviceScheduleCommandRepository.findById(rescheduleCommandcommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND));

        // Care Plan 일정 범위(finishDate)와 소유자(patientId)를 함께 조회
        // servicePreferenceId 기준 단건 조회이므로 사전 검증 시점에 1회만 호출
        CarePlanPort.CarePlanRange carePlanRange = carePlanPort.findCarePlanRange(serviceSchedule.getServicePreferenceId());

        validateOwnership(rescheduleCommandcommand.requesterId(), carePlanRange.patientId());
        validateDeadline(serviceSchedule.getStartedAt());
        validateRescheduleDate(serviceSchedule.getDate(), rescheduleCommandcommand.date(), carePlanRange.finishDate());

        // SCHEDULED 상태 검증 및 RESCHEDULING 전이는 엔티티가 스스로 보장
        serviceSchedule.rescheduling();
        ServiceSchedule saved = serviceScheduleCommandRepository.save(serviceSchedule);

        // ProviderReMatchEvent 발행
        providerReMatchEventPort.publish(
                new ProviderReMatchEventPort.ProviderReMatchEvent(
                        saved.getId(),
                        saved.getServiceOfferingId(),
                        rescheduleCommandcommand.date()
                )
        );

        log.info("[Schedule] 서비스 일정 변경 접수 serviceScheduleId={} requestedDate={}", saved.getId(), rescheduleCommandcommand.date());

        return ServiceScheduleRescheduleResult.from(saved);
    }

    // 본인에게 배정된 일정만 변경 가능
    // Care Plan Internal API 응답의 patientId와 요청자(UserContext) userId를 비교하는 방식으로만 판별
    private void validateOwnership(UUID requesterId, UUID patientId) {
        if (!requesterId.equals(patientId)) {
            throw new BusinessException(ScheduleErrorCode.AUTH_FORBIDDEN);
        }
    }

    // 일정 시작 24시간 전까지만 변경 가능
    private void validateDeadline(LocalDateTime startedAt) {
        if (LocalDateTime.now().plusHours(RESCHEDULE_DEADLINE_HOURS).isAfter(startedAt)) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_DELAY_DEADLINE_EXCEEDED);
        }
    }

    // 기존 일정 날짜 D 기준 하루 앞당기기(D-1) 또는 하루 미루기(D+1)만 허용
    private void validateRescheduleDate(LocalDate currentDate, LocalDate requestedDate, LocalDate finishDate) {
        if (requestedDate.equals(currentDate.minusDays(1))) {
            // 하루 앞당기기: 변경일이 오늘(당일)인 경우 불가
            if (requestedDate.equals(LocalDate.now())) {
                throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_TO_TODAY_NOT_ALLOWED);
            }
            return;
        }

        if (requestedDate.equals(currentDate.plusDays(1))) {
            // 하루 미루기: Care Plan의 일정 범위(finishDate)를 초과할 수 없음
            if (requestedDate.isAfter(finishDate)) {
                throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_EXCEEDS_CARE_PLAN_RANGE);
            }
            return;
        }

        throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_RESCHEDULE_DATE);
    }
}
