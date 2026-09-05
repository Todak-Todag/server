package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultSearchQuery;
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
    // 기능 범위: 서비스 일정 존재 확인 + 배정된 provider 조회 + CommandService에 위임
    public ServiceResultRegisterResult register(ServiceResultRegisterCommand registerCommand) {

        ServiceScheduleResult serviceSchedule = serviceScheduleQueryService.findById(registerCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND));

        // 배정된 서비스 제공자(providerId) 조회 — DB 트랜잭션 밖에서 수행
        UUID assignedProviderId = providerOfferingPort.findAssignedProviderId(serviceSchedule.serviceOfferingId());

        return serviceResultCommandService.register(registerCommand, assignedProviderId);
    }

    // 서비스 수행 결과 목록 조회 유스케이스 조합
    // 08번 문서 "비즈니스 규칙"이 01번에서 확정된 ID 목록 기반 조회 패턴을 그대로 재사용한다고 명시하고 있어,
    // ServiceScheduleFacade.search와 동일한 흐름(역할 분기 → Internal API 1회 호출 → IN 조건 필터링)을 따른다.
    public Page<ServiceResultSearchResult> search(ServiceResultSearchQuery searchQuery) {
        List<UUID> servicePreferenceIds = null;
        List<UUID> serviceOfferingIds = null;

        if (searchQuery.role() == UserRole.PATIENT) {
            // care-plan-service Internal API — GET /internal/v1/service-preferences?patientId=
            servicePreferenceIds = carePlanPort.findServicePreferenceIds(searchQuery.userId());

            // 담당하는 servicePreferenceId가 하나도 없으면 DB 조회 없이 바로 빈 페이지를 반환
            if (servicePreferenceIds.isEmpty()) {
                return Page.empty(searchQuery.pageable());
            }
        } else if (searchQuery.role() == UserRole.SERVICE_PROVIDER) {
            // provider-service Internal API — GET /internal/v1/service-offerings?providerId=
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
