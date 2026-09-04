package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceResultCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceResultFacade {

    private final ServiceScheduleQueryService serviceScheduleQueryService;
    private final ProviderOfferingPort providerOfferingPort;
    private final ServiceResultCommandService serviceResultCommandService;

    // 서비스 수행 결과 등록 유스케이스 조합
    // 기능 범위: 서비스 일정 존재 확인 + 배정된 provider 조회 + CommandService에 위임
    public ServiceResultRegisterResult register(ServiceResultRegisterCommand registerCommand) {

        ServiceScheduleResult serviceSchedule = serviceScheduleQueryService.findById(registerCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND));

        // 배정된 서비스 제공자(providerId) 조회 — DB 트랜잭션 밖에서 수행
        UUID assignedProviderId = providerOfferingPort.findAssignedProviderId(serviceSchedule.serviceOfferingId());

        return serviceResultCommandService.register(registerCommand, assignedProviderId);
    }
}
