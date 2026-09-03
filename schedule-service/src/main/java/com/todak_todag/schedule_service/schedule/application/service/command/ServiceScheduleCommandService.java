package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompleteCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.support.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
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

    private final ServiceScheduleCommandRepository serviceScheduleCommandRepository;
    private final ScheduleOutboxCommandService scheduleOutboxCommandService;
    private final ProviderReMatchEventPayloadSerializer providerReMatchEventPayloadSerializer;
    private final ServiceScheduleValidator serviceScheduleValidator;

    // 서비스 일정 변경
    // 트랜잭션 처리 범위: 검증 + status를 RESCHEDULING으로 변경 + ProviderReMatched 이벤트를 아웃박스에 적재
    @Transactional
    public ServiceScheduleRescheduleResult reschedule(ServiceScheduleRescheduleCommand rescheduleCommand, CarePlanPort.CarePlanRange carePlanRange) {

        // facade가 이미 존재를 확인했지만, facade의 조회와 이 트랜잭션 사이 시점 차이를 방어하기 위해 다시 조회
        ServiceSchedule serviceSchedule = serviceScheduleCommandRepository.findById(rescheduleCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 일정 변경을 위한 검증 진행
        serviceScheduleValidator.validateOwnership(rescheduleCommand.requesterId(), carePlanRange.patientId());
        serviceScheduleValidator.validateDeadline(serviceSchedule.getStartedAt());
        serviceScheduleValidator.validateRescheduleDate(serviceSchedule.getDate(), rescheduleCommand.date(), carePlanRange.finishDate());

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

        // facade가 이미 존재를 확인했지만, facade의 조회와 이 트랜잭션 사이 시점 차이를 방어하기 위해 다시 조회
        ServiceSchedule serviceSchedule = serviceScheduleCommandRepository.findById(cancelCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 일정 취소를 위한 검증 진행
        serviceScheduleValidator.validateOwnership(cancelCommand.requesterId(), carePlanRange.patientId());
        serviceScheduleValidator.validateCancelDeadline(serviceSchedule.getStartedAt());

        // 완료/취소된 일정에 대한 409 처리는 엔티티가 스스로 보장
        serviceSchedule.cancel(cancelCommand.cancelReason());
        ServiceSchedule saved = serviceScheduleCommandRepository.save(serviceSchedule);

        log.info("[Schedule] 서비스 일정 취소 완료 serviceScheduleId={}", saved.getId());

        return ServiceScheduleCancelResult.from(saved);
    }

    // 서비스 수행 완료/부도 처리
    // 트랜잭션 처리 범위: 검증 + status를 COMPLETED 또는 NO_SHOW로 변경
    @Transactional
    public ServiceScheduleCompleteResult complete(ServiceScheduleCompleteCommand completeCommand, UUID assignedProviderId) {

        // facade가 이미 존재를 확인했지만, facade의 조회와 이 트랜잭션 사이 시점 차이를 방어하기 위해 다시 조회
        ServiceSchedule serviceSchedule = serviceScheduleCommandRepository.findById(completeCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 수행 완료 처리를 위한 검증 진행
        serviceScheduleValidator.validateAssignedProvider(completeCommand.requesterId(), assignedProviderId);
        serviceScheduleValidator.validateCompletionDeadline(serviceSchedule.getFinishedAt());

        // SCHEDULED 상태 검증 및 COMPLETED/NO_SHOW 전이는 엔티티가 스스로 보장
        switch (completeCommand.status()) {
            case COMPLETED -> serviceSchedule.complete();
            case NO_SHOW -> serviceSchedule.markNoShow();
        }

        ServiceSchedule saved = serviceScheduleCommandRepository.save(serviceSchedule);

        log.info("[Schedule] 서비스 수행 완료 처리 serviceScheduleId={} status={}", saved.getId(), saved.getStatus());

        return ServiceScheduleCompleteResult.from(saved);
    }
}
