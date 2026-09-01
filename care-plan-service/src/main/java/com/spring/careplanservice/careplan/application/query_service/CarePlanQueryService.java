package com.spring.careplanservice.careplan.application.query_service;


import com.spring.careplanservice.careplan.application.query.CarePlanFindByPatientQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPatientResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServicePreferenceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
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
    private final CarePlanServicePreferenceQueryRepository carePlanServicePreferenceQueryRepository;

    public CarePlanFindByPatientResult findByPatient(
            CarePlanFindByPatientQuery carePlanFindByPatientQuery
    ) {
        CarePlan carePlan = carePlanQueryRepository.findByPatientIdAndStatuses(
                carePlanFindByPatientQuery.patientId(),
                SOCIAL_WORKER_VISIBLE_STATUSES
        ).orElseThrow(() -> new BusinessException(
                ErrorCode.CARE_PLAN_NOT_FOUND,
                Map.of(
                        "reason",
                        "존재하지 않는 patientId입니다."
                )
        ));

        return CarePlanFindByPatientResult.from(carePlan);
    }
}
