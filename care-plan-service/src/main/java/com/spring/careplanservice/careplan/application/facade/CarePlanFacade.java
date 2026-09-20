package com.spring.careplanservice.careplan.application.facade;


import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.port.DischargeQueryPort;
import com.spring.careplanservice.careplan.application.port.ScheduleResultQueryPort;
import com.spring.careplanservice.careplan.application.port.UserQueryPort;
import com.spring.careplanservice.careplan.application.result.*;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.careplan.application.service.query.CarePlanQueryService;
import com.spring.careplanservice.careplan.application.support.CarePlanCompletedEventValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CarePlanFacade {
    private final CarePlanCommandService carePlanCommandService;
    private final CarePlanQueryService carePlanQueryService;
    private final DischargeQueryPort dischargeQueryPort;
    private final UserQueryPort userQueryPort;

    private final ScheduleResultQueryPort scheduleResultQueryPort;
    private final CarePlanCompletedEventValidator carePlanCompletedEventValidator;

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

    public CarePlanStatusUpdateResult updateCarePlanStatus(
            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand
    ) {
        UUID regionId = null;

        if (carePlanStatusUpdateCommand.status() == CarePlanStatus.CONFIRMED) {
            // 외부 User Service 호출을 DB 쓰기 트랜잭션 밖에서 수행하기 위해
            // 먼저 Care Plan의 patientId만 조회한다.
            UUID patientId = carePlanQueryService.findPatientId(
                    carePlanStatusUpdateCommand.carePlanId()
            );

            // User Service 응답 지연이 Care Plan의 DB Connection 점유 시간으로
            // 이어지지 않도록 CommandService의 @Transactional 진입 전에 호출한다.
            UserFindResult userFindResult = userQueryPort.findById(
                    patientId
            );

            // regionId는 CarePlanConfirmed 이벤트 생성 시에만 필요하다.
            regionId = userFindResult.regionId();
        }

        // 실제 상태 변경과 Outbox 저장은 별도의 DB 트랜잭션에서 처리한다.
        return carePlanCommandService.updateCarePlanStatus(
                carePlanStatusUpdateCommand,
                regionId
        );
    }

    public void completeCarePlan(
            CarePlanCompletedEvent carePlanCompletedEvent
    ) {
        // Schedule-Service에서 수신한 완료 이벤트 payload 검증
        carePlanCompletedEventValidator.validatePayload(
                carePlanCompletedEvent
        );

        // 외부 Schedule Service 호출은 DB 쓰기 트랜잭션 진입 전에 수행한다.
        if (carePlanCompletedEvent.serviceResultId() != null) {
            ScheduleResultFindResult scheduleResultFindResult =
                    scheduleResultQueryPort.findById(
                            carePlanCompletedEvent.serviceResultId()
                    );

            // Schedule의 수행 결과가 실제 이 Care Plan에 속하는지 검증
            carePlanCompletedEventValidator.validateCarePlanId(
                    carePlanCompletedEvent,
                    scheduleResultFindResult
            );
        }

        // 실제 상태 변경과 Outbox 저장만 DB 트랜잭션에서 수행
        carePlanCommandService.completeCarePlan(
                carePlanCompletedEvent
        );
    }
}
