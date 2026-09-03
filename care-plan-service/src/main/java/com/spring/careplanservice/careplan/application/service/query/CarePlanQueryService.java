package com.spring.careplanservice.careplan.application.service.query;


import com.spring.careplanservice.careplan.application.query.CarePlanFindByPatientQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanFindByPreferenceQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanFindQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanSearchQuery;
import com.spring.careplanservice.careplan.application.result.*;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.global.common.PageableFactory;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private final CarePlanOwnerValidator carePlanOwnerValidator;

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

    public CarePlanFindResult findCarePlan(
            CarePlanFindQuery carePlanFindQuery
    ) {
        CarePlan carePlan = carePlanQueryRepository
                .findById(carePlanFindQuery.carePlanId())
                .orElseThrow(this::carePlanIdNotFound);

        validateOwner(
                carePlan,
                carePlanFindQuery.userId()
        );

        return CarePlanFindResult.from(carePlan);
    }

    public Page<CarePlanSearchResult> searchCarePlan(
            CarePlanSearchQuery carePlanSearchQuery
    ) {
        validatePage(carePlanSearchQuery.page());

        Pageable pageable = PageableFactory.of(
                carePlanSearchQuery.page(),
                carePlanSearchQuery.size(),
                null
        );

        return carePlanQueryRepository
                .search(carePlanSearchQuery, pageable)
                .map(CarePlanSearchResult::from);
    }

    public ServicePreferenceIdsResult findServicePreferenceIds() {
        return null;
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

    private BusinessException carePlanIdNotFound() {
        return new BusinessException(
                ErrorCode.CARE_PLAN_NOT_FOUND,
                Map.of(
                        "reason",
                        "carePlanId에 해당하는 Care Plan이 존재하지 않습니다."
                )
        );
    }

    private void validatePage(Integer page) {
        if (page != null && page < 0) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_BAD_REQUEST,
                    Map.of(
                            "reason",
                            "page는 0 이상이어야 합니다."
                    )
            );
        }
    }

    // TODO : 추후 분리
    private void validateOwner(
            CarePlan carePlan,
            UUID userId
    ) {
        if (!carePlan.getPatientId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }
    }
}