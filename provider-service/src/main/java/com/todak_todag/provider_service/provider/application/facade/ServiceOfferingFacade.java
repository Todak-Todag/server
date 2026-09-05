package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkDeleteCommand;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkUpdateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.application.port.UserPort;
import com.todak_todag.provider_service.provider.application.query.ServiceOfferingRegionSearchQuery;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkUpdateResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingRegionSearchResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideWorkCommandService;
import com.todak_todag.provider_service.provider.application.service.command.ServiceOfferingCommandService;
import com.todak_todag.provider_service.provider.application.service.query.ServiceOfferingQueryService;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

// 트랜잭션 밖에서 선검증과 외부 서비스 호출을 수행하고, 쓰기 작업만 CommandService에 위임한다
@Component
@RequiredArgsConstructor
public class ServiceOfferingFacade {

    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;
    private final ProvideServiceQueryRepository provideServiceQueryRepository;
    private final ServiceOfferingCommandService serviceOfferingCommandService;
    private final ServiceOfferingQueryService serviceOfferingQueryService;
    private final ProvideWorkCommandService provideWorkCommandService;
    private final UserPort userPort;
    private final SchedulePort schedulePort;

    // 제공 서비스 등록
    // 불필요한 외부 호출을 막기 위해 404·409 검증을 먼저 수행한 뒤 User-Service를 호출한다
    public ServiceOfferingCreateResult create(ServiceOfferingCreateCommand command) {
        if (!provideServiceQueryRepository.existsById(command.provideServiceId())) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_SERVICE_NOT_FOUND);
        }

        if (serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(
                command.providerId(), command.provideServiceId())) {
            throw new BusinessException(ProviderErrorCode.SERVICE_OFFERING_DUPLICATE);
        }

        UUID regionId = userPort.findRegionIdByUserId(command.providerId());

        // p_provide_service_offerings.region_id가 NOT NULL이라 담당 지역이 없으면 저장할 수 없다
        if (regionId == null) {
            throw new BusinessException(ProviderErrorCode.PROVIDER_REGION_NOT_ASSIGNED);
        }

        return serviceOfferingCommandService.create(command, regionId);
    }

    // 제공 서비스 삭제
    // 소유자 검증을 먼저 수행해 권한 없는 요청이 Schedule-Service를 호출하지 않도록 한다
    public void delete(ServiceOfferingDeleteCommand command) {
        // TODO: User-Service 사용자 조회 API 구현 후 ADMIN 담당 지역 검증으로 교체
        //       그 전까지는 ADMIN도 본인 소유만 삭제 가능하도록 제한
        ServiceOffering serviceOffering = findOwnedServiceOffering(command.serviceOfferingId(), command.userId());

        if (schedulePort.existsConfirmedSchedule(serviceOffering.getId())) {
            throw new BusinessException(ProviderErrorCode.SERVICE_OFFERING_SCHEDULE_EXISTS);
        }

        serviceOfferingCommandService.delete(command);
    }

    // 지역별 제공 서비스 목록 조회
    // MASTER는 지역 제한이 없어 User-Service를 호출하지 않는다
    public Page<ServiceOfferingRegionSearchResult> searchByRegion(ServiceOfferingRegionSearchQuery query) {
        validateRegionAccess(query.userId(), query.userRole(), query.regionId());

        return serviceOfferingQueryService.searchByRegion(query);
    }

    // 제공 가능 일정 수정
    // 소유자 검증을 먼저 수행해 권한 없는 요청이 Schedule-Service를 호출하지 않도록 한다
    public ProvideWorkUpdateResult updateProvideWork(ProvideWorkUpdateCommand command) {
        ServiceOffering serviceOffering = findOwnedServiceOffering(command.serviceOfferingId(), command.providerId());

        if (schedulePort.existsConfirmedSchedule(serviceOffering.getId())) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_WORK_SCHEDULE_EXISTS);
        }

        return provideWorkCommandService.update(command);
    }

    // 제공 가능 일정 삭제
    // 소유자 검증을 먼저 수행해 권한 없는 요청이 Schedule-Service를 호출하지 않도록 한다
    public void deleteProvideWork(ProvideWorkDeleteCommand command) {
        ServiceOffering serviceOffering = findOwnedServiceOffering(command.serviceOfferingId(), command.providerId());

        if (schedulePort.existsConfirmedSchedule(serviceOffering.getId())) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_WORK_SCHEDULE_EXISTS);
        }

        provideWorkCommandService.delete(command);
    }

    // 제공 서비스를 조회하고 요청자가 소유자인지 검증한다
    private ServiceOffering findOwnedServiceOffering(UUID serviceOfferingId, UUID requesterId) {
        ServiceOffering serviceOffering = serviceOfferingQueryRepository.findById(serviceOfferingId)
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        if (!serviceOffering.isOwnedBy(requesterId)) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        return serviceOffering;
    }

    // MASTER는 지역 제한 없이 전체 조회 가능
    // ADMIN은 자신의 담당 지역만 조회 가능하며, 담당 지역이 지정되지 않았다면 조회할 수 없다
    private void validateRegionAccess(UUID userId, UserRole userRole, UUID regionId) {
        if (userRole == UserRole.MASTER) {
            return;
        }

        UUID adminRegionId = userPort.findRegionIdByUserId(userId);

        if (adminRegionId == null || !adminRegionId.equals(regionId)) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }
    }
}
