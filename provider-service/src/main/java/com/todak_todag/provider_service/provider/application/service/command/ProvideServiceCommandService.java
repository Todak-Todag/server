package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideServiceCreateCommand;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceCreateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideServiceCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvideServiceCommandService {

    private final ProvideServiceCommandRepository provideServiceCommandRepository;
    private final ProvideServiceQueryRepository provideServiceQueryRepository;

    @Transactional
    public ProvideServiceCreateResult create(ProvideServiceCreateCommand command) {
        if (provideServiceQueryRepository.existsByName(command.name())) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_SERVICE_DUPLICATE);
        }

        ProvideService saved = provideServiceCommandRepository.save(
                ProvideService.of(command.name(), command.content())
        );

        log.info("[Provider] 서비스 종류 등록 provideServiceId={} name={}", saved.getId(), saved.getName());

        return ProvideServiceCreateResult.from(saved);
    }
}