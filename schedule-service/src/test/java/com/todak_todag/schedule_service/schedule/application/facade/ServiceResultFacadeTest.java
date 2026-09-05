package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceResultCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceResultFacadeTest {

    @Mock
    private ServiceScheduleQueryService serviceScheduleQueryService;

    @Mock
    private ProviderOfferingPort providerOfferingPort;

    @Mock
    private ServiceResultCommandService serviceResultCommandService;

    @InjectMocks
    private ServiceResultFacade serviceResultFacade;

    @Test
    @DisplayName("일정을 조회하고 배정된 providerId를 조회한 뒤 CommandService에 위임한다")
    void register_success_delegatesToCommandService() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                serviceScheduleId, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), "비고", providerId
        );
        ServiceScheduleResult scheduleResult = new ServiceScheduleResult(serviceScheduleId, UUID.randomUUID(), serviceOfferingId);
        ServiceResultRegisterResult expected = new ServiceResultRegisterResult(UUID.randomUUID());

        when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.of(scheduleResult));
        when(providerOfferingPort.findAssignedProviderId(serviceOfferingId)).thenReturn(providerId);
        when(serviceResultCommandService.register(command, providerId)).thenReturn(expected);

        // when
        ServiceResultRegisterResult result = serviceResultFacade.register(command);

        // then
        assertThat(result).isEqualTo(expected);
        verify(serviceResultCommandService).register(command, providerId);
    }

    @Test
    @DisplayName("존재하지 않는 일정이면 404를 던지고 provider 조회를 하지 않는다 (07번 문서는 404를 명시적으로 요구)")
    void register_notFound_notFoundWithoutProviderLookup() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                serviceScheduleId, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), null, UUID.randomUUID()
        );
        when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serviceResultFacade.register(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND);

        verify(providerOfferingPort, never()).findAssignedProviderId(any());
        verify(serviceResultCommandService, never()).register(any(), any());
    }
}
