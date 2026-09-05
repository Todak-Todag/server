package com.spring.careplanservice.careplan.application.service.command;


import com.spring.careplanservice.careplan.application.command.ServicePreferenceCreateCommand;
import com.spring.careplanservice.careplan.application.command.ServicePreferenceDeleteCommand;
import com.spring.careplanservice.careplan.application.command.ServicePreferenceUpdateCommand;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceCreateResult;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceUpdateResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.application.support.ServicePreferenceDateValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicePreferenceCommandService {
    private final CarePlanServiceQueryRepository carePlanServiceQueryRepository;
    private final CarePlanCommandRepository carePlanCommandRepository;
    private final ServicePreferenceCommandRepository servicePreferenceCommandRepository;
    private final CarePlanOwnerValidator carePlanOwnerValidator;
    private final ServicePreferenceDateValidator servicePreferenceDateValidator;

    @Transactional
    public ServicePreferenceCreateResult createServicePreference(
            ServicePreferenceCreateCommand servicePreferenceCreateCommand
    ) {
        CarePlanService carePlanService = carePlanServiceQueryRepository
                .findById(servicePreferenceCreateCommand.planServiceId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND
                        )
                );

        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanService.getCarePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        carePlanOwnerValidator.validate(
                servicePreferenceCreateCommand.userId(),
                carePlan.getPatientId()
        );

        validateCarePlanStatus(carePlan);

        servicePreferenceDateValidator.validate(
                servicePreferenceCreateCommand.preferredDate(),
                carePlan.getStartDate(),
                carePlan.getFinishDate()
        );

        CarePlanServicePreference carePlanServicePreference = CarePlanServicePreference.create(
                servicePreferenceCreateCommand.planServiceId(),
                servicePreferenceCreateCommand.preferredDate(),
                servicePreferenceCreateCommand.preferredTimeSlot()
        );

        CarePlanServicePreference savedPreference = servicePreferenceCommandRepository.save(
                carePlanServicePreference
        );

        return ServicePreferenceCreateResult.from(
                savedPreference
        );
    }

    @Transactional
    public ServicePreferenceUpdateResult updateServicePreference(
            ServicePreferenceUpdateCommand servicePreferenceUpdateCommand
    ) {
        CarePlanServicePreference carePlanServicePreference = servicePreferenceCommandRepository
                .findById(servicePreferenceUpdateCommand.servicePreferenceId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.SERVICE_PREFERENCE_NOT_FOUND
                        )
                );

        CarePlanService carePlanService = carePlanServiceQueryRepository
                .findById(carePlanServicePreference.getPlanServiceId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND
                        )
                );

        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanService.getCarePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        carePlanOwnerValidator.validate(
                servicePreferenceUpdateCommand.userId(),
                carePlan.getPatientId()
        );

        validateCarePlanStatus(carePlan);

        servicePreferenceDateValidator.validate(
                servicePreferenceUpdateCommand.preferredDate(),
                carePlan.getStartDate(),
                carePlan.getFinishDate()
        );

        carePlanServicePreference.updatePreference(
                servicePreferenceUpdateCommand.preferredDate(),
                servicePreferenceUpdateCommand.preferredTimeSlot()
        );

        return ServicePreferenceUpdateResult.from(
                carePlanServicePreference
        );
    }

    private void validateCarePlanStatus(
            CarePlan carePlan
    ) {
        if (carePlan.getStatus() != CarePlanStatus.UNDER_REVIEW) {
            throw new BusinessException(ErrorCode.SERVICE_PREFERENCE_NOT_ALLOWED);
        }
    }

    @Transactional
    public void deleteServicePreference(
            ServicePreferenceDeleteCommand servicePreferenceDeleteCommand
    ) {
        CarePlanServicePreference carePlanServicePreference = servicePreferenceCommandRepository
                .findByIdIncludingDeleted(servicePreferenceDeleteCommand.servicePreferenceId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.SERVICE_PREFERENCE_NOT_FOUND
                        )
                );

        CarePlanService carePlanService = carePlanServiceQueryRepository
                .findById(carePlanServicePreference.getPlanServiceId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND
                        )
                );

        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanService.getCarePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        carePlanOwnerValidator.validate(
                servicePreferenceDeleteCommand.userId(),
                carePlan.getPatientId()
        );
        validateNotAlreadyDeleted(carePlanServicePreference);

        validateCarePlanStatusForDelete(carePlan);

        carePlanServicePreference.delete(
                servicePreferenceDeleteCommand.userId()
        );
    }

    // 이미 논리삭제된 희망 일정인지 검사
    private void validateNotAlreadyDeleted(
            CarePlanServicePreference carePlanServicePreference
    ) {
        if (carePlanServicePreference.getDeletedAt() != null) {
            throw new BusinessException(
                    ErrorCode.SERVICE_PREFERENCE_ALREADY_DELETED
            );
        }
    }

    // 삭제 가능한 상태(UNDER_REVIEW)인지 검사
    private void validateCarePlanStatusForDelete(
            CarePlan carePlan
    ) {
        if (carePlan.getStatus() != CarePlanStatus.UNDER_REVIEW) {
            throw new BusinessException(
                    ErrorCode.SERVICE_PREFERENCE_DELETE_NOT_ALLOWED
            );
        }
    }

}
