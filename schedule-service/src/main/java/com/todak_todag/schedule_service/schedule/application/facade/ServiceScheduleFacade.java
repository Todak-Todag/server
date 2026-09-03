package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompleteCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.query.ServiceScheduleDetailQuery;
import com.todak_todag.schedule_service.schedule.application.query.ServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceScheduleCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
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

    // 서비스 일정 상세 조회 유스케이스 조합
    public ServiceScheduleDetailResult detail(ServiceScheduleDetailQuery detailQuery) {

        ServiceScheduleDetailResult detail = serviceScheduleQueryService.findDetailById(detailQuery.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        if (detailQuery.role() == UserRole.PATIENT) {

            // 소유자(patientId) 조회 — DB 트랜잭션 밖에서 수행
            UUID patientId = carePlanPort.findCarePlanRange(detail.servicePreferenceId()).patientId();
            if (!patientId.equals(detailQuery.userId())) {
                throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
            }
        } else if (detailQuery.role() == UserRole.SERVICE_PROVIDER) {

            // 배정된 서비스 제공자(providerId) 조회 — DB 트랜잭션 밖에서 수행
            UUID providerId = providerOfferingPort.findAssignedProviderId(detail.serviceOfferingId());
            if (!providerId.equals(detailQuery.userId())) {
                throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
            }
        } else {
            // 방어 코드
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }

        return detail;
    }

    // 서비스 일정 목록 조회 유스케이스 조합
    public Page<ServiceScheduleSearchResult> search(ServiceScheduleSearchQuery searchQuery) {
        List<UUID> servicePreferenceIds = null;
        List<UUID> serviceOfferingIds = null;

        if (searchQuery.role() == UserRole.PATIENT) {
            servicePreferenceIds = carePlanPort.findServicePreferenceIds(searchQuery.userId());

            // 담당하는 servicePreferenceId가 하나도 없으면 DB 조회 없이 바로 빈 페이지를 반환
            if (servicePreferenceIds.isEmpty()) {
                return Page.empty(searchQuery.pageable());
            }
        } else if (searchQuery.role() == UserRole.SERVICE_PROVIDER) {
            serviceOfferingIds = providerOfferingPort.findServiceOfferingIds(searchQuery.userId());

            // 담당하는 serviceOfferingId가 하나도 없으면 DB 조회 없이 바로 빈 페이지를 반환
            if (serviceOfferingIds.isEmpty()) {
                return Page.empty(searchQuery.pageable());
            }
        } else {
            // 방어 코드
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }

        return serviceScheduleQueryService.search(
                servicePreferenceIds,
                serviceOfferingIds,
                searchQuery.status(),
                searchQuery.date(),
                searchQuery.pageable()
        );
    }
}
