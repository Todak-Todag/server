package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.support.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// 순수한 트랜잭션 경계를 담당
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceScheduleCommandService {

    // 일정 시작 24시간 전까지만 변경 가능
    private static final long RESCHEDULE_DEADLINE_HOURS = 24;

    // 일정 시작 24시간 전까지만 취소 가능
    private static final long CANCEL_DEADLINE_HOURS = 24;

    private final ServiceScheduleCommandRepository serviceScheduleCommandRepository;
    private final ScheduleOutboxCommandService scheduleOutboxCommandService;
    private final ProviderReMatchEventPayloadSerializer providerReMatchEventPayloadSerializer;

    // 서비스 일정 변경
    // 트랜잭션 처리 범위: 검증 + status를 RESCHEDULING으로 변경 + ProviderReMatched 이벤트를 아웃박스에 적재
    @Transactional
    public ServiceScheduleRescheduleResult reschedule(ServiceScheduleRescheduleCommand rescheduleCommand, CarePlanPort.CarePlanRange carePlanRange) {

        // facade가 이미 존재를 확인했지만, facade의 조회와 이 트랜잭션 사이 시점 차이를 방어하기 위해 다시 조회
        ServiceSchedule serviceSchedule = serviceScheduleCommandRepository.findById(rescheduleCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 일정 변경을 위한 검증 진행
        validateOwnership(rescheduleCommand.requesterId(), carePlanRange.patientId());
        validateDeadline(serviceSchedule.getStartedAt());
        validateRescheduleDate(serviceSchedule.getDate(), rescheduleCommand.date(), carePlanRange.finishDate());

        // SCHEDULED 상태 검증 및 RESCHEDULING 전이는 엔티티가 스스로 보장
        serviceSchedule.rescheduling();
        ServiceSchedule saved = serviceScheduleCommandRepository.save(serviceSchedule);

        // ProviderReMatchEvent를 같은 트랜잭션 안에서 아웃박스에 적재 (실제 발행은 릴레이가 트랜잭션 밖에서 수행)
        String payload = providerReMatchEventPayloadSerializer.serialize(
                new ProviderReMatchEventPort.ProviderReMatchEvent(
                        saved.getId(),
                        saved.getServiceOfferingId(),
                        rescheduleCommand.date()
                )
        );

        scheduleOutboxCommandService.enqueue(ProviderReMatchEventPort.EVENT_TYPE, saved.getId(), payload);

        log.info("[Schedule] 서비스 일정 변경 접수 serviceScheduleId={} requestedDate={}", saved.getId(), rescheduleCommand.date());

        return ServiceScheduleRescheduleResult.from(saved);
    }

    // 서비스 일정 취소
    // 트랜잭션 처리 범위: 검증 + status를 CANCELED로 변경
    @Transactional
    public ServiceScheduleCancelResult cancel(ServiceScheduleCancelCommand cancelCommand, CarePlanPort.CarePlanRange carePlanRange) {
        ServiceSchedule serviceSchedule = serviceScheduleCommandRepository.findById(cancelCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 일정 취소를 위한 검증 진행
        validateOwnership(cancelCommand.requesterId(), carePlanRange.patientId());
        validateCancelDeadline(serviceSchedule.getStartedAt());

        // 완료/취소된 일정에 대한 409 처리는 엔티티가 스스로 보장
        serviceSchedule.cancel(cancelCommand.cancelReason());
        ServiceSchedule saved = serviceScheduleCommandRepository.save(serviceSchedule);

        log.info("[Schedule] 서비스 일정 취소 완료 serviceScheduleId={}", saved.getId());

        return ServiceScheduleCancelResult.from(saved);
    }

    // 일정 시작 24시간 전까지만 취소 가능
    private void validateCancelDeadline(LocalDateTime startedAt) {
        if (LocalDateTime.now().plusHours(CANCEL_DEADLINE_HOURS).isAfter(startedAt)) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_CANCEL_DEADLINE_EXCEEDED);
        }
    }

    // 본인에게 배정된 일정만 변경/취소 가능
    // Care Plan Internal API 응답의 patientId와 요청자(UserContext) userId를 비교하는 방식으로만 판별
    private void validateOwnership(UUID requesterId, UUID patientId) {
        if (!requesterId.equals(patientId)) {
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
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
