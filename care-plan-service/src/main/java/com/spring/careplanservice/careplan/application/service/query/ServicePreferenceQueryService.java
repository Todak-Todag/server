package com.spring.careplanservice.careplan.application.service.query;

import com.spring.careplanservice.careplan.application.query.ServicePreferenceFindQuery;
import com.spring.careplanservice.careplan.application.query.ServicePreferenceSearchQuery;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceFindResult;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceSearchResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceView;
import com.spring.careplanservice.global.common.PageableFactory;
import com.spring.careplanservice.global.common.UserRole;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicePreferenceQueryService {
    private final CarePlanQueryRepository carePlanQueryRepository;
    private final CarePlanServiceQueryRepository carePlanServiceQueryRepository;
    private final ServicePreferenceQueryRepository servicePreferenceQueryRepository;
    private final CarePlanOwnerValidator carePlanOwnerValidator;

    public Page<ServicePreferenceSearchResult> searchServicePreferences(
            ServicePreferenceSearchQuery servicePreferenceSearchQuery
    ) {
        CarePlan carePlan = carePlanQueryRepository
                .findById(servicePreferenceSearchQuery.carePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        if (servicePreferenceSearchQuery.role() == UserRole.PATIENT) {
            carePlanOwnerValidator.validate(
                    servicePreferenceSearchQuery.userId(),
                    carePlan.getPatientId()
            );
        }

        // TODO: (MVP 이후) HOSPITAL_STAFF는 본인이 등록한 퇴원건에 연결된 Care Plan만 조회 가능하도록 검증 필요.
        // Discharge Internal API 응답에 hospitalStaffId가 없어 현재 검증 불가.
        // 응답 확장 후 discharge.hospitalStaffId == userId 검증 추가.

        // TODO: (MVP 이후) SOCIAL_WORKER는 본인이 담당 중인 퇴원예정자의 Care Plan만 조회 가능하도록 검증 필요.
        // Social Worker 담당 관계 조회 Internal API 연동 후
        // patientId + socialWorkerId + ACTIVE 상태 기준 검증 추가.

        Pageable pageable = PageableFactory.of(
                servicePreferenceSearchQuery.page(),
                servicePreferenceSearchQuery.size(),
                null
        );

        return servicePreferenceQueryRepository.search(
                servicePreferenceSearchQuery.carePlanId(),
                servicePreferenceSearchQuery.preferredDate(),
                pageable
        ).map(this::toResult);
    }

    private ServicePreferenceSearchResult toResult(
            ServicePreferenceView view
    ) {
        return new ServicePreferenceSearchResult(
                view.servicePreferenceId(),
                view.provideServiceId(),
                view.preferredDate(),
                view.preferredTimeSlot(),
                view.createdAt()
        );
    }

    public ServicePreferenceFindResult findServicePreference(
            ServicePreferenceFindQuery servicePreferenceFindQuery
    ) {
        CarePlanServicePreference carePlanServicePreference = servicePreferenceQueryRepository
                .findById(servicePreferenceFindQuery.servicePreferenceId())
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

        CarePlan carePlan = carePlanQueryRepository
                .findById(carePlanService.getCarePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        if (servicePreferenceFindQuery.role() != UserRole.PATIENT) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        carePlanOwnerValidator.validate(
                servicePreferenceFindQuery.userId(),
                carePlan.getPatientId()
        );

        // TODO: (MVP 이후) HOSPITAL_STAFF는 본인이 등록한 퇴원건에 연결된 Care Plan만 조회 가능하도록 검증 필요.
        // carePlan.getDischargeId() 기준 Discharge Service에서 hospitalStaffId == userId 검증.
        // 현재 Discharge Internal API 응답에 hospitalStaffId가 없어 추후 연동 필요.

        // TODO: (MVP 이후) SOCIAL_WORKER는 본인이 담당 중인 퇴원예정자의 Care Plan만 조회 가능하도록 검증 필요.
        // carePlan.getPatientId() 기준 담당 관계(ACTIVE) 검증.
        // 현재 담당 관계 조회용 Internal API가 없어 추후 연동 필요.

        return new ServicePreferenceFindResult(
                carePlanServicePreference.getId(),
                carePlanServicePreference.getPlanServiceId(),
                carePlanService.getProvideServiceId(),
                carePlanServicePreference.getPreferredDate(),
                carePlanServicePreference.getPreferredTimeSlot(),
                carePlanServicePreference.getCreatedAt()
        );
    }
}
