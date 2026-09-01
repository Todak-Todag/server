package com.todak_todag.provider_service.provider.application.command_service;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCommand;
import com.todak_todag.provider_service.provider.application.port.UserPort;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingResult;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOfferingCommandService {

    private final ServiceOfferingCommandRepository serviceOfferingCommandRepository;
    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;
    private final ProvideServiceQueryRepository provideServiceQueryRepository;
    private final UserPort userPort;

    @Transactional
    public ServiceOfferingResult.Create create(ServiceOfferingCommand.Create command) {
        UUID providerId = command.providerId();
        UUID provideServiceId = command.provideServiceId();

        provideServiceQueryRepository.findById(provideServiceId)
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.PROVIDE_SERVICE_NOT_FOUND));

        if (serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId)) {
            throw new BusinessException(ProviderErrorCode.SERVICE_OFFERING_DUPLICATE);
        }

        UUID regionId = userPort.findRegionIdByUserId(providerId);

        ServiceOffering saved = serviceOfferingCommandRepository.save(
                ServiceOffering.of(providerId, provideServiceId, regionId)
        );

        log.info("[Provider] 제공 서비스 등록 serviceOfferingId={}", saved.getId());

        return ServiceOfferingResult.Create.from(saved);
    }
}