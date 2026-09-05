package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkCreateCommand;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideWorkCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvideWorkCommandService {

    private final ProvideWorkCommandRepository provideWorkCommandRepository;
    private final ProvideWorkQueryRepository provideWorkQueryRepository;
    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    @Transactional
    public ProvideWorkCreateResult create(ProvideWorkCreateCommand command) {
        ServiceOffering serviceOffering = serviceOfferingQueryRepository.findById(command.serviceOfferingId())
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        if (!serviceOffering.isOwnedBy(command.providerId())) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        validateNotOverlapped(command);

        ProvideWork saved = provideWorkCommandRepository.save(
                ProvideWork.of(command.serviceOfferingId(), command.day(), command.startedAt(), command.finishedAt())
        );

        log.info("[Provider] 제공 가능 일정 등록 provideWorkId={} serviceOfferingId={}",
                saved.getId(), command.serviceOfferingId());

        return ProvideWorkCreateResult.from(saved);
    }

    // 같은 제공 서비스 안에서 요일이 같고 시간이 겹치는 일정은 등록할 수 없다
    private void validateNotOverlapped(ProvideWorkCreateCommand command) {
        List<ProvideWork> provideWorks =
                provideWorkQueryRepository.findAllByServiceOfferingId(command.serviceOfferingId());

        boolean overlapped = provideWorks.stream()
                .anyMatch(provideWork ->
                        provideWork.overlaps(command.day(), command.startedAt(), command.finishedAt()));

        if (overlapped) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);
        }
    }
}