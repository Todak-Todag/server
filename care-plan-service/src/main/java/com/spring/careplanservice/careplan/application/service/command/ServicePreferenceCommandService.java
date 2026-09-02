package com.spring.careplanservice.careplan.application.service.command;


import com.spring.careplanservice.careplan.application.command.ServicePreferenceCreateCommand;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceCreateResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
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

    @Transactional
    public ServicePreferenceCreateResult createServicePreference(
            ServicePreferenceCreateCommand servicePreferenceCreateCommand
    ) {
        CarePlanService carePlanService = carePlanServiceQueryRepository.findById(servicePreferenceCreateCommand.planServiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_PLAN_NOT_FOUND));

        CarePlan carePlan = carePlanCommandRepository.findById(carePlanService.getCarePlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_PLAN_NOT_FOUND));

        // TODO : 기존 Care Plan 관련 PR merge 후
        // CarePlanServiceCommandService의 소유자 검증도
        // CarePlanOwnerValidator로 통일
        carePlanOwnerValidator.validate(
                servicePreferenceCreateCommand.userId(),
                carePlan.getPatientId()
        );

        CarePlanServicePreference carePlanServicePreference = CarePlanServicePreference.create(
                servicePreferenceCreateCommand.planServiceId(),
                servicePreferenceCreateCommand.preferredDate(),
                servicePreferenceCreateCommand.preferredTimeSlot()
        );

        CarePlanServicePreference savedPreference = servicePreferenceCommandRepository.save(carePlanServicePreference);

        return ServicePreferenceCreateResult.from(savedPreference);
    }
}
