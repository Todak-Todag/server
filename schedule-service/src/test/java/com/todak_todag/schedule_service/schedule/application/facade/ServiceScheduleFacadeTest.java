package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceScheduleCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceScheduleQueryService;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ServiceScheduleFacade는 (1) QueryService 조회 (2) CarePlanPort 외부 조회 (3) CommandService 위임 순서를
// 올바르게 오케스트레이션하는지만 검증한다. 실제 검증/전이 로직은 ServiceScheduleCommandServiceTest 참고.
// Facade는 domain/repository를 직접 호출하지 않고 Query/Command Service만 사용하므로, 이 두 Service를 Mock으로 대체한다.
@ExtendWith(MockitoExtension.class)
class ServiceScheduleFacadeTest {

    @Mock
    private ServiceScheduleQueryService serviceScheduleQueryService;

    @Mock
    private CarePlanPort carePlanPort;

    @Mock
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @InjectMocks
    private ServiceScheduleFacade serviceScheduleFacade;

    @Test
    void 일정을_조회하고_Care_Plan을_조회한_뒤_CommandService에_위임한다() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        LocalDate requestedDate = LocalDate.now().plusDays(2);

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(serviceScheduleId, requestedDate, requesterId);
        ServiceScheduleResult scheduleResult = new ServiceScheduleResult(serviceScheduleId, servicePreferenceId);
        CarePlanPort.CarePlanRange carePlanRange =
                new CarePlanPort.CarePlanRange(UUID.randomUUID(), requestedDate.plusDays(10), requesterId);
        ServiceScheduleRescheduleResult expected = new ServiceScheduleRescheduleResult(serviceScheduleId, ScheduleStatus.RESCHEDULING);

        when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.of(scheduleResult));
        when(carePlanPort.findCarePlanRange(servicePreferenceId)).thenReturn(carePlanRange);
        when(serviceScheduleCommandService.reschedule(command, carePlanRange)).thenReturn(expected);

        // when
        ServiceScheduleRescheduleResult result = serviceScheduleFacade.reschedule(command);

        // then
        assertThat(result).isEqualTo(expected);
        verify(serviceScheduleCommandService).reschedule(eq(command), eq(carePlanRange));
    }

    @Test
    void 존재하지_않는_일정이면_403을_던지고_Care_Plan을_조회하지_않는다_리소스_존재_비노출() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(
                serviceScheduleId, LocalDate.now().plusDays(1), UUID.randomUUID()
        );
        when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serviceScheduleFacade.reschedule(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

        verify(carePlanPort, never()).findCarePlanRange(any());
        verify(serviceScheduleCommandService, never()).reschedule(any(), any());
    }

    @Test
    void 취소_요청도_일정을_조회하고_Care_Plan을_조회한_뒤_CommandService에_위임한다() {
        // given
        UUID servicePreferenceId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(UUID.randomUUID(), "개인 사정", requesterId);
        ServiceScheduleResult scheduleResult = new ServiceScheduleResult(command.serviceScheduleId(), servicePreferenceId);
        CarePlanPort.CarePlanRange carePlanRange =
                new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.now().plusDays(10), requesterId);
        ServiceScheduleCancelResult expected = new ServiceScheduleCancelResult(command.serviceScheduleId(), LocalDateTime.now());

        when(serviceScheduleQueryService.findById(command.serviceScheduleId())).thenReturn(Optional.of(scheduleResult));
        when(carePlanPort.findCarePlanRange(servicePreferenceId)).thenReturn(carePlanRange);
        when(serviceScheduleCommandService.cancel(command, carePlanRange)).thenReturn(expected);

        // when
        ServiceScheduleCancelResult result = serviceScheduleFacade.cancel(command);

        // then
        assertThat(result).isEqualTo(expected);
        verify(serviceScheduleCommandService).cancel(command, carePlanRange);
    }

    @Test
    void 취소_대상_일정이_존재하지_않으면_403을_던지고_Care_Plan을_조회하지_않는다_리소스_존재_비노출() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(serviceScheduleId, "개인 사정", UUID.randomUUID());
        when(serviceScheduleQueryService.findById(serviceScheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serviceScheduleFacade.cancel(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

        verify(carePlanPort, never()).findCarePlanRange(any());
        verify(serviceScheduleCommandService, never()).cancel(any(), any());
    }
}
