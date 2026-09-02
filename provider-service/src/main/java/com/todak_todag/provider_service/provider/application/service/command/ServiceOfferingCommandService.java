package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.application.port.UserPort;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOfferingCommandService {

    private final ServiceOfferingCommandRepository serviceOfferingCommandRepository;
    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;
    private final ProvideWorkQueryRepository provideWorkQueryRepository;
    private final ProvideServiceQueryRepository provideServiceQueryRepository;
    private final UserPort userPort;
    private final SchedulePort schedulePort;

    @Transactional
    public ServiceOfferingCreateResult create(ServiceOfferingCreateCommand command) {
        UUID providerId = command.providerId();
        UUID provideServiceId = command.provideServiceId();

        if (!provideServiceQueryRepository.existsById(provideServiceId)) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_SERVICE_NOT_FOUND);
        }

        if (serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId)) {
            throw new BusinessException(ProviderErrorCode.SERVICE_OFFERING_DUPLICATE);
        }

        UUID regionId = userPort.findRegionIdByUserId(providerId);

        ServiceOffering saved = serviceOfferingCommandRepository.save(
                ServiceOffering.of(providerId, provideServiceId, regionId)
        );

        log.info("[Provider] 제공 서비스 등록 serviceOfferingId={}", saved.getId());

        return ServiceOfferingCreateResult.from(saved);
    }

    @Transactional
    public void delete(ServiceOfferingDeleteCommand command) {
        ServiceOffering serviceOffering = serviceOfferingQueryRepository.findById(command.serviceOfferingId())
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        validateDeletable(serviceOffering, command);

        if (schedulePort.existsConfirmedSchedule(serviceOffering.getId())) {
            throw new BusinessException(ProviderErrorCode.SERVICE_OFFERING_SCHEDULE_EXISTS);
        }

        List<ProvideWork> provideWorks = provideWorkQueryRepository.findAllByServiceOfferingId(serviceOffering.getId());
        provideWorks.forEach(provideWork -> provideWork.markDeleted(command.userId()));
        serviceOffering.markDeleted(command.userId());

        log.info("[Provider] 제공 서비스 삭제 serviceOfferingId={} provideWorkCount={}",
                serviceOffering.getId(), provideWorks.size());
    }

    private void validateDeletable(ServiceOffering serviceOffering, ServiceOfferingDeleteCommand command) {
        if (command.userRole() == UserRole.ADMIN) {
            // ADMIN은 담당 지역 내에서만 삭제 가능
            // TODO: User-Service 사용자 조회 API 구현 후 담당 지역 검증 추가
            return;
        }

        if (!serviceOffering.isOwnedBy(command.userId())) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }
    }
}