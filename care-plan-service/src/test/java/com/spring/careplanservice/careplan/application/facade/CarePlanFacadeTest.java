package com.spring.careplanservice.careplan.application.facade;

import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.event.ScheduleStatus;
import com.spring.careplanservice.careplan.application.port.DischargeQueryPort;
import com.spring.careplanservice.careplan.application.port.ScheduleResultQueryPort;
import com.spring.careplanservice.careplan.application.port.UserQueryPort;
import com.spring.careplanservice.careplan.application.result.ScheduleResultFindResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.careplan.application.support.CarePlanCompletedEventValidator;
import com.spring.careplanservice.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class CarePlanFacadeTest {
    UUID carePlanId = UUID.randomUUID();
    UUID serviceResultId = UUID.randomUUID();

    @Mock
    private CarePlanCommandService carePlanCommandService;

    @Mock
    private DischargeQueryPort dischargeQueryPort;

    @InjectMocks
    private CarePlanFacade carePlanFacade;

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private ScheduleResultQueryPort scheduleResultQueryPort;

    @Spy
    private CarePlanCompletedEventValidator carePlanCompletedEventValidator =
            new CarePlanCompletedEventValidator();

    @Test
    @DisplayName("완료 이벤트는 Schedule 수행 결과 검증 후 CommandService를 호출")
    void completeCarePlan_success() {
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                carePlanId,
                serviceResultId,
                ScheduleStatus.COMPLETED
        );

        ScheduleResultFindResult scheduleResult = new ScheduleResultFindResult(
                serviceResultId,
                carePlanId
        );

        given(scheduleResultQueryPort.findById(serviceResultId)).willReturn(scheduleResult);

        carePlanFacade.completeCarePlan(event);

        verify(scheduleResultQueryPort).findById(serviceResultId);
        verify(carePlanCommandService).completeCarePlan(event);
    }

    @Test
    @DisplayName("CANCELED 이벤트의 serviceResultId가 null이면 Schedule 조회 없이 완료")
    void completeCarePlan_canceledWithoutResult_success() {
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                carePlanId,
                null,
                ScheduleStatus.CANCELED
        );

        carePlanFacade.completeCarePlan(event);

        verify(scheduleResultQueryPort, never()).findById(any());

        verify(carePlanCommandService).completeCarePlan(event);
    }

    @Test
    @DisplayName("COMPLETED 이벤트에 serviceResultId가 없으면 CommandService를 호출하지 않음")
    void completeCarePlan_completedWithoutResult_fail() {
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                carePlanId,
                null,
                ScheduleStatus.COMPLETED
        );

        assertThatThrownBy(() -> carePlanFacade.completeCarePlan(event)
        ).isInstanceOf(BusinessException.class);

        verify(scheduleResultQueryPort, never()).findById(any());
        verify(carePlanCommandService, never()).completeCarePlan(any());
    }

    @Test
    @DisplayName("Schedule 수행 결과의 carePlanId가 다르면 CommandService를 호출하지 않음")
    void completeCarePlan_carePlanIdMismatch_fail() {
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                carePlanId,
                serviceResultId,
                ScheduleStatus.COMPLETED
        );

        given(scheduleResultQueryPort.findById(serviceResultId))
                .willReturn(
                        new ScheduleResultFindResult(
                                serviceResultId,
                                UUID.randomUUID()
                        )
                );

        assertThatThrownBy(() ->
                carePlanFacade.completeCarePlan(event)
        ).isInstanceOf(BusinessException.class);

        verify(carePlanCommandService, never()).completeCarePlan(any());
    }
}