package com.spring.careplanservice.careplan.application.query_service;


import com.spring.careplanservice.careplan.application.query.CarePlanFindByPatientQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanFindByPreferenceQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPatientResult;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPreferenceResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarePlanQueryService {
    private static final Set<CarePlanStatus> SOCIAL_WORKER_VISIBLE_STATUSES = Set.of(
            CarePlanStatus.CONFIRMED,
            CarePlanStatus.IN_PROGRESS,
            CarePlanStatus.COMPLETED
    );
    private final CarePlanQueryRepository carePlanQueryRepository;
    private final CarePlanServiceQueryRepository carePlanServiceQueryRepository;
    private final ServicePreferenceQueryRepository servicePreferenceQueryRepository;

    public CarePlanFindByPatientResult findByPatient(
            CarePlanFindByPatientQuery carePlanFindByPatientQuery
    ) {
        CarePlan carePlan = carePlanQueryRepository.findByPatientIdAndStatuses(
                carePlanFindByPatientQuery.patientId(),
                SOCIAL_WORKER_VISIBLE_STATUSES
        ).orElseThrow(this::patientNotFound);

        return CarePlanFindByPatientResult.from(carePlan);
    }

    public CarePlanFindByPreferenceResult findByServicePreference(
            CarePlanFindByPreferenceQuery carePlanFindByServicePreferenceQuery
    ) {
        CarePlanServicePreference carePlanServicePreference = servicePreferenceQueryRepository
                .findById(carePlanFindByServicePreferenceQuery.servicePreferenceId())
                .orElseThrow(this::carePlanNotFound);

        CarePlanService carePlanService = carePlanServiceQueryRepository
                .findById(carePlanServicePreference.getPlanServiceId())
                .orElseThrow(this::carePlanNotFound);

        CarePlan carePlan = carePlanQueryRepository
                .findById(carePlanService.getCarePlanId())
                .orElseThrow(this::carePlanNotFound);

        return CarePlanFindByPreferenceResult.from(carePlan);
    }

    private BusinessException carePlanNotFound() {
        return new BusinessException(
                ErrorCode.CARE_PLAN_NOT_FOUND,
                Map.of(
                        "reason",
                        "해당 서비스 희망 일정에 연결된 Care Plan이 존재하지 않습니다."
                )
        );
    }

    private BusinessException patientNotFound() {
        return new BusinessException(
                ErrorCode.CARE_PLAN_NOT_FOUND,
                Map.of(
                        "reason",
                        "존재하지 않는 patientId입니다."
                )
        );
    }
}