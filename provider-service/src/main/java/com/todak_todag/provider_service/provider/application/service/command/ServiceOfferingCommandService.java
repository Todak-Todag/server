package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// 트랜잭션 경계만 담당한다
// 선검증과 외부 서비스 호출은 ServiceOfferingFacade가 트랜잭션 밖에서 수행한다
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOfferingCommandService {

    private final ServiceOfferingCommandRepository serviceOfferingCommandRepository;
    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;
    private final ProvideWorkQueryRepository provideWorkQueryRepository;

    // regionId는 Facade가 트랜잭션 밖에서 User-Service로부터 조회해 전달한다
    @Transactional
    public ServiceOfferingCreateResult create(ServiceOfferingCreateCommand command, UUID regionId) {
        // Facade의 검증과 이 트랜잭션 사이의 시점 차이를 방어하기 위해 중복을 다시 확인
        if (serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(
                command.providerId(), command.provideServiceId())) {
            throw new BusinessException(ProviderErrorCode.SERVICE_OFFERING_DUPLICATE);
        }

        ServiceOffering saved = serviceOfferingCommandRepository.save(
                ServiceOffering.of(command.providerId(), command.provideServiceId(), regionId)
        );

        log.info("[Provider] 제공 서비스 등록 serviceOfferingId={}", saved.getId());

        return ServiceOfferingCreateResult.from(saved);
    }

    @Transactional
    public void delete(ServiceOfferingDeleteCommand command) {
        // Facade의 조회와 이 트랜잭션 사이의 시점 차이를 방어하기 위해 다시 조회
        ServiceOffering serviceOffering = serviceOfferingQueryRepository.findById(command.serviceOfferingId())
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        if (!serviceOffering.isOwnedBy(command.userId())) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        List<ProvideWork> provideWorks = provideWorkQueryRepository.findAllByServiceOfferingId(serviceOffering.getId());
        provideWorks.forEach(provideWork -> provideWork.markDeleted(command.userId()));
        serviceOffering.markDeleted(command.userId());

        log.info("[Provider] 제공 서비스 삭제 serviceOfferingId={} provideWorkCount={}",
                serviceOffering.getId(), provideWorks.size());
    }
}
