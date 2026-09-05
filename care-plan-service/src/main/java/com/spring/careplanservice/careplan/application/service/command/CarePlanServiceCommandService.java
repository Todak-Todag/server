package com.spring.careplanservice.careplan.application.service.command;


import com.spring.careplanservice.careplan.application.command.CarePlanServiceCancelCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanServiceSelectCommand;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSelectResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarePlanServiceCommandService {
    private final CarePlanCommandRepository carePlanCommandRepository;
    private final CarePlanServiceCommandRepository carePlanServiceCommandRepository;
    private final ServicePreferenceCommandRepository servicePreferenceCommandRepository;
    private final CarePlanOwnerValidator carePlanOwnerValidator;

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

        carePlanOwnerValidator.validate(
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

    @Transactional
    public void cancelCarePlanService(
            CarePlanServiceCancelCommand carePlanServiceCancelCommand
    ) {
        CarePlanService carePlanService = carePlanServiceCommandRepository
                .findById(carePlanServiceCancelCommand.planServiceId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND
                        )
                );

        validateNotAlreadyDeleted(carePlanService);

        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanService.getCarePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        carePlanOwnerValidator.validate(
                carePlanServiceCancelCommand.userId(),
                carePlan.getPatientId()
        );

        validateCancelable(carePlan);

        List<CarePlanServicePreference> preferences = servicePreferenceCommandRepository.findAllByPlanServiceIds(
                List.of(carePlanServiceCancelCommand.planServiceId())
        );

        UUID deletedBy = carePlanServiceCancelCommand.userId();

        preferences.forEach(preference -> preference.delete(deletedBy));
        carePlanService.delete(deletedBy);
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
                .existsByCarePlanIdAndProvideServiceIdAndCreatedBy(
                        carePlanId,
                        provideServiceId,
                        userId
                )) {

            throw new BusinessException(
                    ErrorCode.CARE_PLAN_SERVICE_ALREADY_EXISTS
            );
        }
    }

    // 이미 논리삭제된 서비스 항목인지 검사
    private void validateNotAlreadyDeleted(
            CarePlanService carePlanService
    ) {
        if (carePlanService.getDeletedAt() != null) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_SERVICE_ALREADY_DELETED
            );
        }
    }

    // 취소 가능한 상태(UNDER_REVIEW)인지 검사
    private void validateCancelable(
            CarePlan carePlan
    ) {
        if (!carePlan.isUnderReview()) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_SERVICE_CANCEL_NOT_ALLOWED
            );
        }
    }

}
