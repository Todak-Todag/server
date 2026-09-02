package com.spring.careplanservice.careplan.application.service.command;


import com.spring.careplanservice.careplan.application.command.CarePlanServiceSelectCommand;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSelectResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarePlanServiceCommandService {
    private final CarePlanCommandRepository carePlanCommandRepository;
    private final CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @Transactional
    public CarePlanServiceSelectResult selectCarePlanService(
            CarePlanServiceSelectCommand carePlanServiceSelectCommand
    ) {
        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanServiceSelectCommand.carePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        validateCarePlanOwner(
                carePlanServiceSelectCommand.userId(),
                carePlan.getPatientId()
        );

        validateDuplicateCarePlanService(
                carePlanServiceSelectCommand.carePlanId(),
                carePlanServiceSelectCommand.provideServiceId(),
                carePlanServiceSelectCommand.userId()
        );

        CarePlanService carePlanService = CarePlanService.create(
                carePlanServiceSelectCommand.carePlanId(),
                carePlanServiceSelectCommand.provideServiceId()
        );

        CarePlanService savedCarePlanService = carePlanServiceCommandRepository.save(carePlanService);

        return CarePlanServiceSelectResult.from(savedCarePlanService);
    }


    // 요청한 환자가 해당 Care Plan의 환자인지 검사
    private void validateCarePlanOwner(
            UUID userId,
            UUID patientId
    ) {
        if (!userId.equals(patientId)) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }
    }

    // 동일한 서비스가 이미 선택되어 있는지 검사
    private void validateDuplicateCarePlanService(
            UUID carePlanId,
            UUID provideServiceId,
            UUID userId
    ) {
        if (carePlanServiceCommandRepository
                .existsByCarePlanIdAndProvideServiceId(
                        carePlanId,
                        provideServiceId,
                        userId
                )) {

            throw new BusinessException(
                    ErrorCode.CARE_PLAN_SERVICE_ALREADY_EXISTS
            );
        }
    }

}
