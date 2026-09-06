package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultDetailQuery;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultSearchResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceResultCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceResultQueryService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceResultFacade {

    private final ServiceScheduleQueryService serviceScheduleQueryService;
    private final ProviderOfferingPort providerOfferingPort;
    private final ServiceResultCommandService serviceResultCommandService;
    private final CarePlanPort carePlanPort;
    private final ServiceResultQueryService serviceResultQueryService;

    // 서비스 수행 결과 등록 유스케이스 조합
    public ServiceResultRegisterResult register(ServiceResultRegisterCommand registerCommand) {
        ServiceScheduleResult scheduleResult = serviceScheduleQueryService.findById(registerCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 배정된 서비스 제공자(providerId) 조회 — DB 트랜잭션 밖에서 수행
        UUID assignedProviderId = providerOfferingPort.findAssignedProviderId(scheduleResult.serviceOfferingId());

        return serviceResultCommandService.register(registerCommand, assignedProviderId);
    }

    // 서비스 수행 결과 상세 조회 유스케이스 조합
    public ServiceResultDetailResult detail(ServiceResultDetailQuery detailQuery) {
        ServiceResultDetailResult detailResult = serviceResultQueryService.findDetailById(detailQuery.serviceResultId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // serviceScheduleId로 서비스 일정 조회 — serviceOfferingId/servicePreferenceId 확보용
        ServiceScheduleResult scheduleResult = serviceScheduleQueryService.findById(detailResult.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 역할 분기 후 Internal API 점검증 — Internal API 호출은 DB 트랜잭션 밖에서 수행
        if (detailQuery.role() == UserRole.PATIENT) {
            UUID patientId = carePlanPort.findCarePlanRange(scheduleResult.servicePreferenceId()).patientId();

            if (!patientId.equals(detailQuery.userId())) {
                throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
            }
        } else if (detailQuery.role() == UserRole.SERVICE_PROVIDER) {
            UUID providerId = providerOfferingPort.findAssignedProviderId(scheduleResult.serviceOfferingId());

            if (!providerId.equals(detailQuery.userId())) {
                throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
            }
        } else {
            // 방어 코드
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }

        return detailResult;
    }

    // 서비스 수행 결과 목록 조회 유스케이스 조합
    public Page<ServiceResultSearchResult> search(ServiceResultSearchQuery searchQuery) {
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

        return serviceResultQueryService.search(
                servicePreferenceIds,
                serviceOfferingIds,
                searchQuery.pageable()
        );
    }
}
