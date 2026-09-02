package com.spring.careplanservice.careplan.application.service.command;


import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarePlanCommandService {
    private static final long CARE_PLAN_PERIOD_DAYS = 30L;

    private final CarePlanCommandRepository carePlanCommandRepository;
    private final CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @Transactional
    public CarePlanCreateResult createCarePlan(
            CarePlanCreateCommand carePlanCreateCommand,
            DischargeFindResult dischargeFindResult
    ) {
        validateDuplicateCarePlan(carePlanCreateCommand.dischargeId());

        validatePatient(
                carePlanCreateCommand.patientId(),
                dischargeFindResult.patientId()
        );

        validateActualDate(dischargeFindResult.actualDate());

        LocalDate startDate = dischargeFindResult.actualDate().plusDays(1);

        LocalDate finishDate = startDate.plusDays(CARE_PLAN_PERIOD_DAYS - 1);

        CarePlan carePlan = CarePlan.create(
                carePlanCreateCommand.patientId(),
                carePlanCreateCommand.dischargeId(),
                startDate,
                finishDate,
                carePlanCreateCommand.note()
        );

        CarePlan savedCarePlan = carePlanCommandRepository.save(carePlan);

        List<CarePlanService> carePlanServices = carePlanCreateCommand.provideServiceIds()
                .stream()
                .distinct()
                .map(provideServiceId ->
                        CarePlanService.create(
                                savedCarePlan.getId(),
                                provideServiceId
                        )
                )
                .toList();

        carePlanServiceCommandRepository.saveAll(
                carePlanServices
        );

        return CarePlanCreateResult.from(
                savedCarePlan
        );
    }

    private void validateDuplicateCarePlan(
            UUID dischargeId
    ) {
        if (carePlanCommandRepository.existsByDischargeId(dischargeId)) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_ALREADY_EXISTS
            );
        }
    }

    private void validatePatient(
            UUID requestPatientId,
            UUID dischargePatientId
    ) {
        if (!requestPatientId.equals(dischargePatientId)) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_PATIENT_MISMATCH
            );
        }
    }

    private void validateActualDate(
            LocalDate actualDate
    ) {
        if (actualDate == null) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_NOT_COMPLETED
            );
        }
    }
}
