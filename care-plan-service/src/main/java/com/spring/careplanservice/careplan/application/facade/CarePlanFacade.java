package com.spring.careplanservice.careplan.application.facade;


import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.port.DischargeQueryPort;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanFacade {
    private final CarePlanCommandService carePlanCommandService;
    private final DischargeQueryPort dischargeQueryPort;

    public CarePlanCreateResult createCarePlan(
            CarePlanCreateCommand carePlanCreateCommand
    ) {
        DischargeFindResult dischargeFindResult = dischargeQueryPort.findById(
                carePlanCreateCommand.dischargeId()
        );

        return carePlanCommandService.createCarePlan(
                carePlanCreateCommand,
                dischargeFindResult
        );
    }
}
