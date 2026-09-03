package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompleteCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceScheduleCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceScheduleFacade {

    private final ServiceScheduleQueryService serviceScheduleQueryService;
    private final CarePlanPort carePlanPort;
    private final ProviderOfferingPort providerOfferingPort;
    private final ServiceScheduleCommandService serviceScheduleCommandService;

    // 서비스 일정 변경 유스케이스 조합
    // 기능 범위: 검증 + status를 RESCHEDULING으로 변경 + ProviderReMatched 이벤트 발행
    public ServiceScheduleRescheduleResult reschedule(ServiceScheduleRescheduleCommand rescheduleCommand) {

        // 존재 확인 겸 servicePreferenceId 확보 - QueryService는 조회만, 존재 여부 판단은 Facade 책임
        ServiceScheduleResult serviceSchedule = serviceScheduleQueryService.findById(rescheduleCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // Care Plan 일정 범위(finishDate)와 소유자(patientId) 조회 — DB 트랜잭션 밖에서 수행
        CarePlanPort.CarePlanRange carePlanRange = carePlanPort.findCarePlanRange(serviceSchedule.servicePreferenceId());

        return serviceScheduleCommandService.reschedule(rescheduleCommand, carePlanRange);
    }

    // 서비스 일정 취소 유스케이스 조합
    // 기능 범위: 검증 + status를 CANCELED로 변경
    public ServiceScheduleCancelResult cancel(ServiceScheduleCancelCommand cancelCommand) {

        ServiceScheduleResult serviceSchedule = serviceScheduleQueryService.findById(cancelCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 소유자(patientId) 조회 — DB 트랜잭션 밖에서 수행
        CarePlanPort.CarePlanRange carePlanRange = carePlanPort.findCarePlanRange(serviceSchedule.servicePreferenceId());

        return serviceScheduleCommandService.cancel(cancelCommand, carePlanRange);
    }

    // 서비스 수행 완료/부도 처리 유스케이스 조합
    // 기능 범위: 검증 + status를 COMPLETED 또는 NO_SHOW로 변경
    public ServiceScheduleCompleteResult complete(ServiceScheduleCompleteCommand completeCommand) {

        ServiceScheduleResult serviceSchedule = serviceScheduleQueryService.findById(completeCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 배정된 서비스 제공자(providerId) 조회 — DB 트랜잭션 밖에서 수행
        UUID assignedProviderId = providerOfferingPort.findAssignedProviderId(serviceSchedule.serviceOfferingId());

        return serviceScheduleCommandService.complete(completeCommand, assignedProviderId);
    }
}
