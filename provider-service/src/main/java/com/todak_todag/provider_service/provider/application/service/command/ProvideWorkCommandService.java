package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkUpdateCommand;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkUpdateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideWorkCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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

        validateNotOverlapped(
                command.serviceOfferingId(),
                null,
                command.day(),
                command.startedAt(),
                command.finishedAt()
        );

        ProvideWork saved = provideWorkCommandRepository.save(
                ProvideWork.of(command.serviceOfferingId(), command.day(), command.startedAt(), command.finishedAt())
        );

        log.info("[Provider] 제공 가능 일정 등록 provideWorkId={} serviceOfferingId={}",
                saved.getId(), command.serviceOfferingId());

        return ProvideWorkCreateResult.from(saved);
    }

    // schedulePort 호출은 Facade가 트랜잭션 밖에서 수행한다
    @Transactional
    public ProvideWorkUpdateResult update(ProvideWorkUpdateCommand command) {
        ServiceOffering serviceOffering = serviceOfferingQueryRepository.findById(command.serviceOfferingId())
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        if (!serviceOffering.isOwnedBy(command.providerId())) {
            throw new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN);
        }

        ProvideWork provideWork = provideWorkQueryRepository.findById(command.provideWorkId())
                .orElseThrow(() -> new BusinessException(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND));

        if (!provideWork.getServiceOfferingId().equals(command.serviceOfferingId())) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND);
        }

        validateNotOverlapped(
                command.serviceOfferingId(),
                command.provideWorkId(),
                command.day(),
                command.startedAt(),
                command.finishedAt()
        );

        provideWork.update(command.day(), command.startedAt(), command.finishedAt());

        log.info("[Provider] 제공 가능 일정 수정 provideWorkId={} serviceOfferingId={}",
                provideWork.getId(), command.serviceOfferingId());

        return ProvideWorkUpdateResult.from(provideWork);
    }

    // 같은 제공 서비스 안에서 요일이 같고 시간이 겹치는 일정은 등록·수정할 수 없다
    // 수정인 경우 자기 자신은 겹침 대상에서 제외한다
    private void validateNotOverlapped(
            UUID serviceOfferingId,
            UUID excludedProvideWorkId,
            Integer day,
            LocalTime startedAt,
            LocalTime finishedAt
    ) {
        List<ProvideWork> provideWorks =
                provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId);

        boolean overlapped = provideWorks.stream()
                .filter(provideWork -> excludedProvideWorkId == null
                        || !excludedProvideWorkId.equals(provideWork.getId()))
                .anyMatch(provideWork -> provideWork.overlaps(day, startedAt, finishedAt));

        if (overlapped) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);
        }
    }
}