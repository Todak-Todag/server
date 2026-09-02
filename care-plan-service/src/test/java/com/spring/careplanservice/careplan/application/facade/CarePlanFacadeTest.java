package com.spring.careplanservice.careplan.application.facade;

import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.port.DischargeQueryPort;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.global.common.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class CarePlanFacadeTest {
    UUID userId = UUID.randomUUID();

    @Mock
    private CarePlanCommandService carePlanCommandService;

    @Mock
    private DischargeQueryPort dischargeQueryPort;

    @InjectMocks
    private CarePlanFacade carePlanFacade;

    @Test
    @DisplayName("퇴원 건 조회 후 Care Plan 생성 서비스 호출")
    void createCarePlan_success() {
        UUID patientId = UUID.randomUUID();
        UUID dischargeId = UUID.randomUUID();

        CarePlanCreateCommand command = new CarePlanCreateCommand(
                patientId,
                dischargeId,
                null,
                null,
                userId,
                UserRole.PATIENT
        );

        DischargeFindResult discharge = new DischargeFindResult(
                dischargeId,
                patientId,
                LocalDate.of(2026, 8, 1)
        );

        CarePlanCreateResult result = new CarePlanCreateResult(UUID.randomUUID());

        given(dischargeQueryPort.findById(dischargeId)).willReturn(discharge);
        given(carePlanCommandService.createCarePlan(command, discharge)).willReturn(result);

        CarePlanCreateResult actual = carePlanFacade.createCarePlan(command);

        assertThat(actual).isEqualTo(result);

        verify(dischargeQueryPort).findById(dischargeId);
        verify(carePlanCommandService).createCarePlan(command, discharge);
    }
}